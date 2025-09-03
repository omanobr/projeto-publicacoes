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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

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
    @PostMapping("/upload-extract-text")
    public ResponseEntity<String> extrairTextoDeArquivo(@RequestParam("file") MultipartFile file) {
        try {
            String textoExtraido = publicacaoService.extrairTextoDeArquivo(file);
            return ResponseEntity.ok(textoExtraido);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao processar o arquivo.");
        }
    }
    // VVV--- ATUALIZE O MÉTODO @GetMapping ---VVV
    @GetMapping
    public List<PublicacaoListDTO> listarPublicacoes(@RequestParam(required = false) String termo) {
        if (termo != null && !termo.isEmpty()) {
            return publicacaoService.searchPublicacoes(termo);
        }
        return publicacaoService.findAllAsListDto();
    }
    // ^^^--- FIM DA ATUALIZAÇÃO ---^^^


    // VVV--- ADICIONE O NOVO ENDPOINT DE BUSCA AVANÇADA ---VVV
    @GetMapping("/busca-avancada")
    public List<PublicacaoListDTO> buscarPublicacaoPorConteudo(@RequestParam String conteudo) {
        return publicacaoService.searchPublicacoesAvancado(conteudo);
    }
    // ^^^--- FIM DO NOVO ENDPOINT ---^^^
}

