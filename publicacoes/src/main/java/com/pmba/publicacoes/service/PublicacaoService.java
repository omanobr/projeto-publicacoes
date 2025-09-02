package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.*;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.TipoVinculo;
import com.pmba.publicacoes.model.VinculoNormativo;
import com.pmba.publicacoes.repository.PublicacaoRepository;
import com.pmba.publicacoes.repository.VinculoNormativoRepository;
import org.hibernate.Hibernate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicacaoService {

    @Autowired
    private PublicacaoRepository publicacaoRepository;
    @Autowired
    private VinculoNormativoRepository vinculoRepository;

    private Safelist safelistConfigurado() {
        return Safelist.basic().addAttributes("span", "data-meta", "style").addTags("del", "ins");
    }

    @Transactional
    public void criarComMetadadosExtraidos(CriacaoPublicacaoDTO dadosRecebidos) {
        String htmlRecebido = dadosRecebidos.getConteudoHtml();
        Document doc = Jsoup.parse(htmlRecebido);
        Element tituloElement = doc.selectFirst("span[data-meta=titulo]");

        String tituloFinal;
        if (tituloElement != null) {
            tituloFinal = tituloElement.text();
        } else {
            tituloFinal = "Título não definido";
        }

        String sanitizedHtml = Jsoup.clean(htmlRecebido, safelistConfigurado());

        publicacaoRepository.criarNovaPublicacao(
                tituloFinal,
                dadosRecebidos.getNumero(),
                dadosRecebidos.getTipo(),
                dadosRecebidos.getDataPublicacao(),
                sanitizedHtml
        );
    }

    @Transactional
    public PublicacaoDetailDTO atualizarComMetadadosExtraidos(Long id, Publicacao dadosRecebidos) {
        Publicacao publicacaoParaSalvar = publicacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicação não encontrada com id: " + id));
        String htmlRecebido = dadosRecebidos.getConteudoHtml();
        Document doc = Jsoup.parse(htmlRecebido);
        Element tituloElement = doc.selectFirst("span[data-meta=titulo]");
        if (tituloElement != null) {
            publicacaoParaSalvar.setTitulo(tituloElement.text());
        }
        String sanitizedHtml = Jsoup.clean(htmlRecebido, safelistConfigurado());
        publicacaoParaSalvar.setNumero(dadosRecebidos.getNumero());
        publicacaoParaSalvar.setTipo(dadosRecebidos.getTipo());
        publicacaoParaSalvar.setDataPublicacao(dadosRecebidos.getDataPublicacao());
        publicacaoParaSalvar.setConteudoHtml(sanitizedHtml);
        Publicacao salvo = publicacaoRepository.save(publicacaoParaSalvar);
        return convertToDetailDto(salvo);
    }

    @Transactional(readOnly = true)
    public PublicacaoDetailDTO findByIdProcessado(Long id) {
        Publicacao publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicação não encontrada com id: " + id));
        PublicacaoDetailDTO dto = convertToDetailDto(publicacao);
        String conteudoProcessado = processarVinculos(publicacao, false);
        dto.setConteudoHtml(conteudoProcessado);
        return dto;
    }

    @Transactional(readOnly = true)
    public PublicacaoEditDTO findByIdForEditing(Long id) {
        Publicacao publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicação não encontrada com id: " + id));

        Hibernate.initialize(publicacao.getVinculosGerados());

        PublicacaoEditDTO dto = new PublicacaoEditDTO();
        dto.setId(publicacao.getId());
        dto.setTitulo(publicacao.getTitulo());
        dto.setNumero(publicacao.getNumero());
        dto.setTipo(publicacao.getTipo());
        dto.setDataPublicacao(publicacao.getDataPublicacao());
        dto.setStatus(publicacao.getStatus());
        String conteudoParaEdicao = processarVinculos(publicacao, true);
        dto.setConteudoHtml(conteudoParaEdicao);
        List<VinculoSimpleDTO> vinculosGerados = publicacao.getVinculosGerados().stream()
                .map(this::convertVinculoToSimpleDto)
                .collect(Collectors.toList());
        dto.setVinculosGerados(vinculosGerados);
        List<VinculoNormativo> vinculosRecebidosEntities = vinculoRepository.findAllByPublicacaoDestinoId(id);
        List<VinculoSimpleDTO> vinculosRecebidos = vinculosRecebidosEntities.stream()
                .map(this::convertVinculoToSimpleDto)
                .collect(Collectors.toList());
        dto.setVinculosRecebidos(vinculosRecebidos);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PublicacaoListDTO> findAllAsListDto() {
        // ANTES:
        // List<Publicacao> publicacoes = publicacaoRepository.findAll();
        // return publicacoes.stream()
        //         .map(this::convertToListDto)
        //         .collect(Collectors.toList());

        // DEPOIS (mais simples, rápido e corrige o bug):
        return publicacaoRepository.findAllForListView();
    }

    private String processarVinculos(Publicacao publicacao, boolean apenasTachado) {
        // A inicialização dos vínculos recebidos acontece aqui, dentro da transação.
        List<VinculoNormativo> vinculosRecebidos = vinculoRepository.findAllByPublicacaoDestinoId(publicacao.getId());
        String conteudoProcessado = publicacao.getConteudoHtml();
        if (!vinculosRecebidos.isEmpty()) {
            for (var vinculo : vinculosRecebidos) {
                String textoOriginal = vinculo.getTextoDoTrecho();
                if (textoOriginal == null || textoOriginal.isEmpty()) continue;
                String textoSubstituto;
                if (vinculo.getTipoVinculo() == TipoVinculo.ALTERA && vinculo.getTextoNovo() != null && !apenasTachado) {
                    textoSubstituto = String.format(
                            "<a href=\"/publicacao/%d\" class=\"vinculo-link\" title=\"Redação alterada pela Publicação %s\"><del>%s</del> <br> <ins>%s</ins></a>",
                            vinculo.getPublicacaoOrigem().getId(),
                            vinculo.getPublicacaoOrigem().getNumero(),
                            textoOriginal,
                            vinculo.getTextoNovo()
                    );
                } else {
                    if (apenasTachado) {
                        textoSubstituto = String.format("<del>%s</del>", textoOriginal);
                    } else {
                        textoSubstituto = String.format(
                                "<a href=\"/publicacao/%d\" class=\"vinculo-link\" title=\"Trecho revogado pela Publicação %s\"><del>%s</del></a>",
                                vinculo.getPublicacaoOrigem().getId(),
                                vinculo.getPublicacaoOrigem().getNumero(),
                                textoOriginal
                        );
                    }
                }
                conteudoProcessado = conteudoProcessado.replace(textoOriginal, textoSubstituto);
            }
        }
        return conteudoProcessado;
    }

    private VinculoSimpleDTO convertVinculoToSimpleDto(VinculoNormativo vinculo) {
        VinculoSimpleDTO dto = new VinculoSimpleDTO();
        dto.setId(vinculo.getId());
        dto.setTipoVinculo(vinculo.getTipoVinculo());
        dto.setTextoDoTrecho(vinculo.getTextoDoTrecho());
        dto.setPublicacaoOrigemId(vinculo.getPublicacaoOrigem().getId());
        dto.setPublicacaoOrigemTitulo(vinculo.getPublicacaoOrigem().getTitulo());
        dto.setPublicacaoDestinoId(vinculo.getPublicacaoDestino().getId());
        dto.setPublicacaoDestinoTitulo(vinculo.getPublicacaoDestino().getTitulo());
        return dto;
    }

    private PublicacaoDetailDTO convertToDetailDto(Publicacao publicacao) {
        PublicacaoDetailDTO dto = new PublicacaoDetailDTO();
        dto.setId(publicacao.getId());
        dto.setTitulo(publicacao.getTitulo());
        dto.setNumero(publicacao.getNumero());
        dto.setTipo(publicacao.getTipo());
        dto.setDataPublicacao(publicacao.getDataPublicacao());
        dto.setConteudoHtml(publicacao.getConteudoHtml());
        dto.setStatus(publicacao.getStatus());
        return dto;
    }

    private PublicacaoListDTO convertToListDto(Publicacao publicacao) {
        PublicacaoListDTO dto = new PublicacaoListDTO();
        dto.setId(publicacao.getId());
        dto.setTitulo(publicacao.getTitulo());
        dto.setNumero(publicacao.getNumero());
        dto.setTipo(publicacao.getTipo());
        dto.setDataPublicacao(publicacao.getDataPublicacao());
        dto.setStatus(publicacao.getStatus());
        return dto;
    }

    private String formatarTipoVinculo(String tipo) {
        return tipo.replace("_", " ").toLowerCase();
    }
}

