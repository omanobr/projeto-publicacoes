package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.StatusPublicacao;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PublicacaoDetailDTO {
    private Long id;
    private String titulo;
    private String numero;
    private String tipo;
    private LocalDate dataPublicacao;
    private String conteudoHtml; // A diferença principal está aqui
    private StatusPublicacao status; // <-- Adicione esta linha
}