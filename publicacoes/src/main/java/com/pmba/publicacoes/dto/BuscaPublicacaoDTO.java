package com.pmba.publicacoes.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BuscaPublicacaoDTO {
    private String numero;
    private String conteudo;
    private Integer ano;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
}
