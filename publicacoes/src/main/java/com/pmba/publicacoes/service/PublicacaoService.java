package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.*;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.TipoVinculo;
import com.pmba.publicacoes.model.VinculoNormativo;
import com.pmba.publicacoes.repository.PublicacaoRepository;
import com.pmba.publicacoes.repository.VinculoNormativoRepository;
import jakarta.persistence.criteria.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.hibernate.Hibernate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        String tituloFinal = (tituloElement != null) ? tituloElement.text() : "Título não definido";
        String sanitizedHtml = Jsoup.clean(htmlRecebido, safelistConfigurado());

        publicacaoRepository.criarNovaPublicacao(
                tituloFinal,
                dadosRecebidos.getNumero(),
                dadosRecebidos.getTipo(),
                dadosRecebidos.getDataPublicacao(),
                sanitizedHtml,
                dadosRecebidos.getBgo()
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
        publicacaoParaSalvar.setBgo(dadosRecebidos.getBgo());
        Publicacao salvo = publicacaoRepository.save(publicacaoParaSalvar);
        return convertToDetailDto(salvo, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public PublicacaoDetailDTO findByIdProcessado(Long id) {
        Publicacao publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicação não encontrada com id: " + id));

        Hibernate.initialize(publicacao.getVinculosGerados());
        List<VinculoNormativo> vinculosRecebidos = vinculoRepository.findAllByPublicacaoDestinoId(id);

        Set<VinculoPublicacaoInfoDTO> publicacoesVinculadas = Stream.concat(
                publicacao.getVinculosGerados().stream().map(v -> new VinculoPublicacaoInfoDTO(v.getPublicacaoDestino().getId(), v.getPublicacaoDestino().getTitulo())),
                vinculosRecebidos.stream().map(v -> new VinculoPublicacaoInfoDTO(v.getPublicacaoOrigem().getId(), v.getPublicacaoOrigem().getTitulo()))
        ).collect(Collectors.toSet());

        PublicacaoDetailDTO dto = convertToDetailDto(publicacao, new ArrayList<>(publicacoesVinculadas));
        String conteudoProcessado = processarVinculos(publicacao, false, vinculosRecebidos);
        dto.setConteudoHtml(conteudoProcessado);
        return dto;
    }

    @Transactional(readOnly = true)
    public PublicacaoEditDTO findByIdForEditing(Long id) {
        Publicacao publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicação não encontrada com id: " + id));

        Hibernate.initialize(publicacao.getVinculosGerados());
        List<VinculoNormativo> vinculosRecebidosEntities = vinculoRepository.findAllByPublicacaoDestinoId(id);

        PublicacaoEditDTO dto = new PublicacaoEditDTO();
        dto.setId(publicacao.getId());
        dto.setTitulo(publicacao.getTitulo());
        dto.setNumero(publicacao.getNumero());
        dto.setTipo(publicacao.getTipo());
        dto.setDataPublicacao(publicacao.getDataPublicacao());
        dto.setStatus(publicacao.getStatus());
        dto.setBgo(publicacao.getBgo());

        String conteudoParaEdicao = processarVinculos(publicacao, true, vinculosRecebidosEntities);
        dto.setConteudoHtml(conteudoParaEdicao);

        dto.setVinculosGerados(publicacao.getVinculosGerados().stream()
                .map(this::convertVinculoToSimpleDto)
                .collect(Collectors.toList()));

        dto.setVinculosRecebidos(vinculosRecebidosEntities.stream()
                .map(this::convertVinculoToSimpleDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private String processarVinculos(Publicacao publicacao, boolean apenasTachado, List<VinculoNormativo> vinculosRecebidos) {
        String conteudoProcessado = publicacao.getConteudoHtml();
        if (vinculosRecebidos.isEmpty()) return conteudoProcessado;

        for (var vinculo : vinculosRecebidos) {
            String textoOriginal = vinculo.getTextoDoTrecho();
            if (textoOriginal == null || textoOriginal.isEmpty()) continue;

            String textoSubstituto;

            if (vinculo.getTipoVinculo() == TipoVinculo.ALTERA && vinculo.getTextoNovo() != null && !apenasTachado) {
                String textoNovoInline = vinculo.getTextoNovo().trim()
                        .replaceAll("</p>\\s*<p>", "<br>")
                        .replaceAll("^<p>", "").replaceAll("</p>$", "");

                textoSubstituto = String.format(
                        "<a href=\"/publicacao/%d\" class=\"trecho-alterado\" data-vinculo-info=\"Redação alterada pela Publicação %s\">" +
                                "<del>%s</del><br><ins>%s</ins>" +
                                "</a>",
                        vinculo.getPublicacaoOrigem().getId(),
                        vinculo.getPublicacaoOrigem().getNumero(),
                        textoOriginal,
                        textoNovoInline
                );
            } else {
                if (apenasTachado) {
                    textoSubstituto = String.format("<del>%s</del>", textoOriginal);
                } else {
                    // VVV--- LÓGICA CORRIGIDA AQUI ---VVV
                    // Adiciona a classe "trecho-revogado" para que o CSS possa identificar e aplicar o tooltip.
                    textoSubstituto = String.format(
                            "<a href=\"/publicacao/%d\" class=\"trecho-revogado\" data-vinculo-info=\"Trecho revogado pela Publicação %s\">" +
                                    "<del>%s</del>" +
                                    "</a>",
                            vinculo.getPublicacaoOrigem().getId(),
                            vinculo.getPublicacaoOrigem().getNumero(),
                            textoOriginal
                    );
                }
            }
            conteudoProcessado = conteudoProcessado.replaceFirst(Pattern.quote(textoOriginal), Matcher.quoteReplacement(textoSubstituto));
        }
        return conteudoProcessado;
    }

    @Transactional(readOnly = true)
    public List<PublicacaoListDTO> searchPublicacoes(BuscaPublicacaoDTO dto) {
        Specification<Publicacao> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dto.getNumero() != null && !dto.getNumero().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numero")), "%" + dto.getNumero().toLowerCase() + "%"));
            }
            if (dto.getAno() != null) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.function("YEAR", Integer.class, root.get("dataPublicacao")), dto.getAno()));
            }
            if (dto.getConteudo() != null && !dto.getConteudo().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("conteudoHtml")), "%" + dto.getConteudo().toLowerCase() + "%"));
            }
            if (dto.getDataInicial() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dataPublicacao"), dto.getDataInicial()));
            }
            if (dto.getDataFinal() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dataPublicacao"), dto.getDataFinal()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "dataPublicacao", "id");

        List<Publicacao> publicacoes = publicacaoRepository.findAll(spec, sort);
        return publicacoes.stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
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

    private PublicacaoDetailDTO convertToDetailDto(Publicacao publicacao, List<VinculoPublicacaoInfoDTO> publicacoesVinculadas) {
        PublicacaoDetailDTO dto = new PublicacaoDetailDTO();
        dto.setId(publicacao.getId());
        dto.setTitulo(publicacao.getTitulo());
        dto.setNumero(publicacao.getNumero());
        dto.setTipo(publicacao.getTipo());
        dto.setDataPublicacao(publicacao.getDataPublicacao());
        dto.setConteudoHtml(publicacao.getConteudoHtml());
        dto.setStatus(publicacao.getStatus());
        dto.setBgo(publicacao.getBgo());
        dto.setPublicacoesVinculadas(publicacoesVinculadas);
        return dto;
    }

    private PublicacaoListDTO convertToListDto(Publicacao publicacao) {
        return new PublicacaoListDTO(
                publicacao.getId(),
                publicacao.getTitulo(),
                publicacao.getNumero(),
                publicacao.getTipo(),
                publicacao.getDataPublicacao(),
                publicacao.getStatus()
        );
    }

    public String extrairTextoDeArquivo(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String textoSimples;

        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                textoSimples = stripper.getText(document);
            }
        } else if (originalFilename != null && (originalFilename.toLowerCase().endsWith(".docx"))) {
            try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                textoSimples = extractor.getText();
            }
        } else {
            throw new IllegalArgumentException("Formato de arquivo não suportado.");
        }
        return converterTextoParaHtml(textoSimples);
    }

    private String converterTextoParaHtml(String textoSimples) {
        if (textoSimples == null || textoSimples.trim().isEmpty()) return "";

        String[] lines = textoSimples.split("\\r?\\n");
        StringBuilder htmlResult = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                htmlResult.append("<p>").append(trimmedLine).append("</p>");
            }
        }
        return htmlResult.toString();
    }
}

