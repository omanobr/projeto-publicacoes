package com.pmba.publicacoes.controller;

import com.pmba.publicacoes.dto.CriacaoPublicacaoDTO;
import com.pmba.publicacoes.dto.PublicacaoDetailDTO;
import com.pmba.publicacoes.dto.PublicacaoListDTO;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.service.PublicacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/publicacoes")
public class PublicacaoController {

    @Autowired
    private PublicacaoService publicacaoService;

    @PostMapping
    public ResponseEntity<Void> criarPublicacao(@RequestBody CriacaoPublicacaoDTO publicacaoDTO) {
        publicacaoService.criarComMetadadosExtraidos(
                publicacaoDTO.getNumero(),
                publicacaoDTO.getTipo(),
                publicacaoDTO.getDataPublicacao(),
                publicacaoDTO.getConteudoHtml()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // =================================================================
    // A CORREÇÃO PRINCIPAL ESTÁ AQUI
    // =================================================================
    @GetMapping
    public List<PublicacaoListDTO> listarPublicacoes() {
        // Garantimos que estamos chamando o método de serviço que retorna a lista de DTOs
        return publicacaoService.findAllAsListDto();
    }
    // =================================================================

    @GetMapping("/{id}")
    public PublicacaoDetailDTO buscarPublicacaoPorId(@PathVariable Long id) {
        return publicacaoService.findByIdProcessado(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicacaoDetailDTO> atualizarPublicacao(@PathVariable Long id, @RequestBody Publicacao publicacaoAtualizada) {
        PublicacaoDetailDTO dto = publicacaoService.atualizarComMetadadosExtraidos(id, publicacaoAtualizada);
        return ResponseEntity.ok(dto);
    }
}