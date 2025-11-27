"""
Nayan AI - PyTorch to ONNX Converter
Converts GPT-2 based PyTorch models to ONNX format for mobile inference.
"""

import torch
import torch.nn as nn
from typing import Dict, Any
import os


class GPT2Config:
    """Configuration for Nayan GPT-2 model"""
    def __init__(self):
        self.n_layer = 6
        self.n_head = 6
        self.n_embd = 384
        self.block_size = 1024
        self.vocab_size = 50304
        self.dropout = 0.0


class CausalSelfAttention(nn.Module):
    """Multi-head causal self-attention"""
    def __init__(self, config):
        super().__init__()
        assert config.n_embd % config.n_head == 0
        self.c_attn = nn.Linear(config.n_embd, 3 * config.n_embd)
        self.c_proj = nn.Linear(config.n_embd, config.n_embd)
        self.n_head = config.n_head
        self.n_embd = config.n_embd
        self.register_buffer("bias", torch.tril(torch.ones(config.block_size, config.block_size))
                             .view(1, 1, config.block_size, config.block_size))

    def forward(self, x):
        B, T, C = x.size()
        q, k, v = self.c_attn(x).split(self.n_embd, dim=2)
        k = k.view(B, T, self.n_head, C // self.n_head).transpose(1, 2)
        q = q.view(B, T, self.n_head, C // self.n_head).transpose(1, 2)
        v = v.view(B, T, self.n_head, C // self.n_head).transpose(1, 2)
        
        att = (q @ k.transpose(-2, -1)) * (1.0 / (k.size(-1) ** 0.5))
        att = att.masked_fill(self.bias[:, :, :T, :T] == 0, float('-inf'))
        att = torch.nn.functional.softmax(att, dim=-1)
        y = att @ v
        y = y.transpose(1, 2).contiguous().view(B, T, C)
        return self.c_proj(y)


class MLP(nn.Module):
    """Feed-forward network"""
    def __init__(self, config):
        super().__init__()
        self.c_fc = nn.Linear(config.n_embd, 4 * config.n_embd)
        self.gelu = nn.GELU()
        self.c_proj = nn.Linear(4 * config.n_embd, config.n_embd)

    def forward(self, x):
        return self.c_proj(self.gelu(self.c_fc(x)))


class Block(nn.Module):
    """Transformer block"""
    def __init__(self, config):
        super().__init__()
        self.ln_1 = nn.LayerNorm(config.n_embd)
        self.attn = CausalSelfAttention(config)
        self.ln_2 = nn.LayerNorm(config.n_embd)
        self.mlp = MLP(config)

    def forward(self, x):
        x = x + self.attn(self.ln_1(x))
        x = x + self.mlp(self.ln_2(x))
        return x


class GPT(nn.Module):
    """GPT-2 Model"""
    def __init__(self, config):
        super().__init__()
        self.config = config
        self.transformer = nn.ModuleDict(dict(
            wte=nn.Embedding(config.vocab_size, config.n_embd),
            wpe=nn.Embedding(config.block_size, config.n_embd),
            h=nn.ModuleList([Block(config) for _ in range(config.n_layer)]),
            ln_f=nn.LayerNorm(config.n_embd),
        ))
        self.lm_head = nn.Linear(config.n_embd, config.vocab_size, bias=False)

    def forward(self, idx):
        device = idx.device
        b, t = idx.size()
        assert t <= self.config.block_size, f"Cannot forward sequence of length {t}, block size is {self.config.block_size}"
        
        pos = torch.arange(0, t, dtype=torch.long, device=device)
        tok_emb = self.transformer.wte(idx)
        pos_emb = self.transformer.wpe(pos)
        x = tok_emb + pos_emb
        
        for block in self.transformer.h:
            x = block(x)
        
        x = self.transformer.ln_f(x)
        logits = self.lm_head(x)
        return logits


def remove_orig_mod_prefix(state_dict):
    """Remove _orig_mod prefix from state dict keys"""
    new_state_dict = {}
    for key, value in state_dict.items():
        new_key = key.replace("_orig_mod.", "")
        new_state_dict[new_key] = value
    return new_state_dict


def convert_to_onnx(input_path: str, output_path: str) -> Dict[str, Any]:
    """
    Convert PyTorch GPT-2 model to ONNX format
    
    Args:
        input_path: Path to .pt file
        output_path: Path to save .onnx file
    
    Returns:
        Dictionary with status and message
    """
    try:
        # Step 1: Validate input file
        if not os.path.exists(input_path):
            return {
                "success": False,
                "message": f"Input file not found: {input_path}"
            }
        
        # Step 2: Load the model architecture
        try:
            config = GPT2Config()
            model = GPT(config)
        except Exception as e:
            return {
                "success": False,
                "message": f"Failed to create model architecture: {str(e)}"
            }
        
        # Step 3: Load checkpoint
        try:
            checkpoint = torch.load(input_path, map_location='cpu')
        except Exception as e:
            return {
                "success": False,
                "message": f"Failed to load checkpoint file: {str(e)}"
            }
        
        # Step 4: Extract state dict
        try:
            if isinstance(checkpoint, dict):
                if 'model' in checkpoint:
                    state_dict = checkpoint['model']
                elif 'state_dict' in checkpoint:
                    state_dict = checkpoint['state_dict']
                else:
                    state_dict = checkpoint
            else:
                state_dict = checkpoint
            
            # Remove _orig_mod prefix if present
            state_dict = remove_orig_mod_prefix(state_dict)
        except Exception as e:
            return {
                "success": False,
                "message": f"Failed to extract state dict: {str(e)}"
            }
        
        # Step 5: Load weights
        try:
            missing_keys, unexpected_keys = model.load_state_dict(state_dict, strict=False)
            model.eval()
        except Exception as e:
            return {
                "success": False,
                "message": f"Failed to load model weights: {str(e)}"
            }
        
        # Step 6: Create dummy input
        try:
            dummy_input = torch.randint(0, config.vocab_size, (1, config.block_size), dtype=torch.long)
        except Exception as e:
            return {
                "success": False,
                "message": f"Failed to create dummy input: {str(e)}"
            }
        
        # Step 7: Export to ONNX
        try:
            torch.onnx.export(
                model,
                dummy_input,
                output_path,
                input_names=['input_ids'],
                output_names=['logits'],
                dynamic_axes={
                    'input_ids': {0: 'batch_size', 1: 'sequence'},
                    'logits': {0: 'batch_size', 1: 'sequence'}
                },
                opset_version=13,  # Changed from 14 to 13 for better compatibility
                do_constant_folding=True,
                verbose=False
            )
        except Exception as e:
            return {
                "success": False,
                "message": f"ONNX export failed: {str(e)}"
            }
        
        # Step 8: Verify output file
        if not os.path.exists(output_path):
            return {
                "success": False,
                "message": "ONNX file was not created"
            }
        
        return {
            "success": True,
            "message": f"Successfully converted model to {output_path}",
            "model_size_mb": os.path.getsize(output_path) / (1024 * 1024)
        }
        
    except Exception as e:
        return {
            "success": False,
            "message": f"Unexpected error: {str(e)}"
        }



def get_model_info(onnx_path: str) -> Dict[str, Any]:
    """Get information about the ONNX model"""
    try:
        if not os.path.exists(onnx_path):
            return {"success": False, "message": "Model file not found"}
        
        size_mb = os.path.getsize(onnx_path) / (1024 * 1024)
        
        return {
            "success": True,
            "size_mb": size_mb,
            "path": onnx_path
        }
    except Exception as e:
        return {"success": False, "message": str(e)}
