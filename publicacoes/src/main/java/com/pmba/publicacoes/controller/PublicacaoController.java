package com.pmba.publicacoes.controller;

import com.pmba.publicacoes.dto.*;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.StatusPublicacao;
import com.pmba.publicacoes.service.PublicacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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

    @GetMapping("/busca")
    public List<PublicacaoListDTO> buscarPublicacoes(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) StatusPublicacao status) {

        BuscaPublicacaoDTO dto = new BuscaPublicacaoDTO();
        dto.setNumero(numero);
        dto.setTermo(termo);
        dto.setAno(ano);
        dto.setDataInicial(dataInicial);
        dto.setDataFinal(dataFinal);
        dto.setStatus(status);

        return publicacaoService.searchPublicacoes(dto);
    }
}

