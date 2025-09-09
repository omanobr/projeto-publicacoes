package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.StatusPublicacao;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BuscaPublicacaoDTO {
    private String numero;
    private Integer ano;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private String termo;
    private StatusPublicacao status;
}

