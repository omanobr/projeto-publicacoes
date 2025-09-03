package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.StatusPublicacao;
import lombok.Data;

import java.time.LocalDate;
import java.util.List; // Importe a classe List

@Data
public class PublicacaoDetailDTO {
    private Long id;
    private String titulo;
    private String numero;
    private String tipo;
    private LocalDate dataPublicacao;
    private String conteudoHtml;
    private StatusPublicacao status;
    private String bgo;

    // VVV--- CAMPO ADICIONADO ---VVV
    // Lista de publicações vinculadas (tanto as que esta publicação afeta, quanto as que afetam esta)
    private List<VinculoPublicacaoInfoDTO> publicacoesVinculadas;
    // ^^^--- FIM DA ADIÇÃO ---^^^
}
