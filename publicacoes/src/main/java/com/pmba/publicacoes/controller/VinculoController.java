package com.pmba.publicacoes.controller;

import com.pmba.publicacoes.dto.CriarVinculoRequestDTO;
import com.pmba.publicacoes.dto.VinculoResponseDTO; // <-- Importe o novo DTO
import com.pmba.publicacoes.service.VinculoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/vinculos")
public class VinculoController {

    @Autowired
    private VinculoService vinculoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // VVV--- TIPO DE RETORNO ATUALIZADO ---VVV
    public VinculoResponseDTO criarVinculo(@RequestBody CriarVinculoRequestDTO request) {
        return vinculoService.criarVinculo(
                request.getPublicacaoOrigemId(),
                request.getPublicacaoDestinoId(),
                request.getTipoVinculo(),
                request.getTextoDoTrecho()
        );
    }

    // VVV--- ADICIONE ESTE NOVO ENDPOINT ---VVV
    @PostMapping("/revogar-totalmente")
    @ResponseStatus(HttpStatus.CREATED)
    public VinculoResponseDTO revogarPublicacaoTotalmente(@RequestBody CriarVinculoRequestDTO request) {
        // Reutilizamos o mesmo DTO de requisição
        return vinculoService.revogarTotalmente(
                request.getPublicacaoOrigemId(),
                request.getPublicacaoDestinoId()
        );
    }

}