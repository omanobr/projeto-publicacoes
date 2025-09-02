package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.CriacaoPublicacaoDTO;
import com.pmba.publicacoes.dto.PublicacaoDetailDTO;
import com.pmba.publicacoes.dto.PublicacaoListDTO;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.StatusPublicacao;
import com.pmba.publicacoes.model.VinculoNormativo;
import com.pmba.publicacoes.repository.PublicacaoRepository;
import com.pmba.publicacoes.repository.VinculoNormativoRepository;
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
        return Safelist.basic().addAttributes("span", "data-meta");
    }

    @Transactional
    public Publicacao criarComMetadadosExtraidos(CriacaoPublicacaoDTO dadosRecebidos) {
        Publicacao publicacaoParaSalvar = new Publicacao();
        publicacaoParaSalvar.setStatus(StatusPublicacao.ATIVA); // Garantindo o status na criação

        String htmlRecebido = dadosRecebidos.getConteudoHtml();
        Document doc = Jsoup.parse(htmlRecebido);
        Element tituloElement = doc.selectFirst("span[data-meta=titulo]");

        if (tituloElement != null) {
            publicacaoParaSalvar.setTitulo(tituloElement.text());
        } else {
            publicacaoParaSalvar.setTitulo("Título não definido");
        }

        String sanitizedHtml = Jsoup.clean(htmlRecebido, safelistConfigurado());
        publicacaoParaSalvar.setConteudoHtml(sanitizedHtml);

        publicacaoParaSalvar.setNumero(dadosRecebidos.getNumero());
        publicacaoParaSalvar.setTipo(dadosRecebidos.getTipo());
        publicacaoParaSalvar.setDataPublicacao(dadosRecebidos.getDataPublicacao());

        return publicacaoRepository.save(publicacaoParaSalvar);
    }

    @Transactional(readOnly = true)
    public List<PublicacaoListDTO> findAllAsListDto() {
        List<Publicacao> publicacoes = publicacaoRepository.findAll();
        // A conversão agora é simples e segura, sem acessar relacionamentos complexos.
        return publicacoes.stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
    }

    // --- MÉTODOS EXISTENTES E AUXILIARES (COMPLETOS) ---

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
        List<VinculoNormativo> vinculosRecebidos = vinculoRepository.findAllByPublicacaoDestinoId(id);
        String conteudoProcessado = publicacao.getConteudoHtml();
        if (!vinculosRecebidos.isEmpty()) {
            for (var vinculo : vinculosRecebidos) {
                String textoOriginal = vinculo.getTextoDoTrecho();
                if (textoOriginal != null && !textoOriginal.isEmpty()) {
                    String textoSubstituto = String.format(
                            "<span class=\"trecho-alterado\" style=\"text-decoration: line-through;\" data-vinculo-info=\"%s pela publicação %s\">%s</span>",
                            formatarTipoVinculo(vinculo.getTipoVinculo().name()),
                            vinculo.getPublicacaoOrigem().getNumero(),
                            textoOriginal
                    );
                    conteudoProcessado = conteudoProcessado.replace(textoOriginal, textoSubstituto);
                }
            }
        }
        PublicacaoDetailDTO dto = convertToDetailDto(publicacao);
        dto.setConteudoHtml(conteudoProcessado);
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
