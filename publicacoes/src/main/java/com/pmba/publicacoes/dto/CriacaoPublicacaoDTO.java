package com.pmba.publicacoes.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CriacaoPublicacaoDTO {
    private String numero;
    private String tipo;
    private LocalDate dataPublicacao;
    private String conteudoHtml;
    private String bgo;
}