package com.pmba.publicacoes.controller;

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

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/publicacoes")
public class PublicacaoController {

    @Autowired
    private PublicacaoService publicacaoService;

    // CORREÇÃO: Este método agora retorna uma resposta vazia, pois a consulta nativa não retorna o objeto criado.
    @PostMapping
    public ResponseEntity<Void> criarPublicacao(@RequestBody CriacaoPublicacaoDTO publicacaoDTO) {
        publicacaoService.criarComMetadadosExtraidos(publicacaoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public List<PublicacaoListDTO> listarPublicacoes() {
        return publicacaoService.findAllAsListDto();
    }

    // Endpoint para a PÁGINA DE VISUALIZAÇÃO
    @GetMapping("/{id}")
    public PublicacaoDetailDTO buscarPublicacaoPorId(@PathVariable Long id) {
        return publicacaoService.findByIdProcessado(id);
    }

    // Endpoint para a PÁGINA DE EDIÇÃO
    @GetMapping("/{id}/for-editing")
    public PublicacaoEditDTO getPublicacaoForEditing(@PathVariable Long id) {
        return publicacaoService.findByIdForEditing(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicacaoDetailDTO> atualizarPublicacao(@PathVariable Long id, @RequestBody Publicacao publicacaoAtualizada) {
        // O método de atualização retorna um DetailDTO, pois é para onde o utilizador é redirecionado
        PublicacaoDetailDTO dto = publicacaoService.atualizarComMetadadosExtraidos(id, publicacaoAtualizada);
        return ResponseEntity.ok(dto);
    }
}

