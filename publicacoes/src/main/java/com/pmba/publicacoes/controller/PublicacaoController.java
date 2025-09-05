package com.pmba.publicacoes.controller;

import com.pmba.publicacoes.dto.BuscaPublicacaoDTO;
import com.pmba.publicacoes.dto.CriacaoPublicacaoDTO;
import com.pmba.publicacoes.dto.PublicacaoDetailDTO;
import com.pmba.publicacoes.dto.PublicacaoEditDTO;
import com.pmba.publicacoes.dto.PublicacaoListDTO;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.service.PublicacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/publicacoes")
public class PublicacaoController {

    @Autowired
    private PublicacaoService publicacaoService;

    @PostMapping
    public ResponseEntity<Void> criarPublicacao(@RequestBody CriacaoPublicacaoDTO publicacaoDTO) {
        publicacaoService.criarComMetadadosExtraidos(publicacaoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public PublicacaoDetailDTO buscarPublicacaoPorId(@PathVariable Long id) {
        return publicacaoService.findByIdProcessado(id);
    }

    @GetMapping("/{id}/for-editing")
    public PublicacaoEditDTO getPublicacaoForEditing(@PathVariable Long id) {
        return publicacaoService.findByIdForEditing(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicacaoDetailDTO> atualizarPublicacao(@PathVariable Long id, @RequestBody Publicacao publicacaoAtualizada) {
        PublicacaoDetailDTO dto = publicacaoService.atualizarComMetadadosExtraidos(id, publicacaoAtualizada);
        return ResponseEntity.ok(dto);
    }
    @PostMapping("/upload-extract-text")
    public ResponseEntity<String> extrairTextoDeArquivo(@RequestParam("file") MultipartFile file) {
        try {
            String textoExtraido = publicacaoService.extrairTextoDeArquivo(file);
            return ResponseEntity.ok(textoExtraido);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao processar o arquivo.");
        }
    }

    // Endpoint unificado para busca e listagem inicial
    @GetMapping("/busca")
    public List<PublicacaoListDTO> buscarPublicacoes(BuscaPublicacaoDTO dto) { // Recebe o DTO com os parâmetros
        return publicacaoService.searchPublicacoes(dto);
    }
}

