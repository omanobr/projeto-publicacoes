package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.*;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.TipoVinculo;
import com.pmba.publicacoes.model.VinculoNormativo;
import com.pmba.publicacoes.repository.PublicacaoRepository;
import com.pmba.publicacoes.repository.VinculoNormativoRepository;
import jakarta.persistence.criteria.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
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

import java.awt.geom.Rectangle2D;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.function.Function;


import java.io.IOException;
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

    private String processarVinculos(Publicacao publicacao, boolean paraEdicao, List<VinculoNormativo> vinculosRecebidos) {
        Document doc = Jsoup.parse(publicacao.getConteudoHtml());

        for (VinculoNormativo vinculo : vinculosRecebidos) {
            TipoVinculo tipo = vinculo.getTipoVinculo();
            String textoOriginal = vinculo.getTextoDoTrecho();
            String textoNovo = vinculo.getTextoNovo();

            if (tipo == TipoVinculo.REVOGA_PARCIALMENTE && textoOriginal != null) {
                String spanRevogado = String.format(
                        "<a href=\"/publicacao/%d\" class=\"trecho-revogado\" data-vinculo-info=\"Trecho revogado pela Publicação %s\"><del>%s</del></a>",
                        vinculo.getPublicacaoOrigem().getId(),
                        vinculo.getPublicacaoOrigem().getNumero(),
                        textoOriginal
                );
                String htmlDoc = doc.html().replace(textoOriginal, spanRevogado);
                doc = Jsoup.parse(htmlDoc);
            }

            if (tipo == TipoVinculo.ALTERA && textoNovo != null && !paraEdicao) {
                for (Element p : doc.select("p, span, div")) {
                    if (p.text().contains(textoNovo)) {
                        String anotacaoHtml = String.format(
                                "<div class=\"anotacao-alteracao\">" +
                                        "  <p>Alterado pela <a href=\"/publicacao/%d\">Publicação nº %s</a>.</p>" +
                                        "  <p><strong>Redação original:</strong> \"%s\"</p>" +
                                        "</div>",
                                vinculo.getPublicacaoOrigem().getId(),
                                vinculo.getPublicacaoOrigem().getNumero(),
                                textoOriginal
                        );
                        p.after(anotacaoHtml);
                        break;
                    }
                }
            }
        }

        return doc.body().html();
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

            // VVV--- CORREÇÃO AQUI: BUSCA POR TERMO NO TÍTULO E NO CONTEÚDO ---VVV
            if (dto.getConteudo() != null && !dto.getConteudo().isEmpty()) {
                String termoBusca = "%" + dto.getConteudo().toLowerCase() + "%";
                Predicate buscaTitulo = criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), termoBusca);
                Predicate buscaConteudo = criteriaBuilder.like(criteriaBuilder.lower(root.get("conteudoHtml")), termoBusca);
                predicates.add(criteriaBuilder.or(buscaTitulo, buscaConteudo));
            }
            // ^^^--- FIM DA CORREÇÃO ---^^^

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
            StringBuilder textoCompleto = new StringBuilder();
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                for (PDPage page : document.getPages()) {
                    float width = page.getMediaBox().getWidth();
                    float height = page.getMediaBox().getHeight();

                    // VVV--- AJUSTE AQUI ---VVV
                    float margemSuperior = 80; // Espaço a ignorar no topo
                    float margemInferior = 40;  // Espaço a ignorar na base

                    float y = margemSuperior;
                    float alturaRegiao = height - (margemSuperior + margemInferior);

                    Rectangle2D.Float region = new Rectangle2D.Float(0, y, width, alturaRegiao);
                    // ^^^--- FIM DO AJUSTE ---^^^

                    String regionName = "contentArea";
                    stripper.addRegion(regionName, region);

                    stripper.extractRegions(page);
                    textoCompleto.append(stripper.getTextForRegion(regionName));
                }
            }
            textoSimples = textoCompleto.toString();

        } else if (originalFilename != null && (originalFilename.toLowerCase().endsWith(".docx"))) {
            XWPFDocument document = new XWPFDocument(file.getInputStream());
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            textoSimples = extractor.getText();
            extractor.close();
        } else {
            throw new IllegalArgumentException("Formato de arquivo não suportado.");
        }

        return converterTextoParaHtml(textoSimples);
    }

    private String converterTextoParaHtml(String textoSimples) {
        if (textoSimples == null || textoSimples.trim().isEmpty()) {
            return "";
        }

        String[] lines = textoSimples.split("\\r?\\n");
        List<String> rawParagraphs = new ArrayList<>();
        StringBuilder paragraphBuilder = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            if (trimmedLine.isEmpty()) {
                if (paragraphBuilder.length() > 0) {
                    rawParagraphs.add(paragraphBuilder.toString().trim());
                    paragraphBuilder.setLength(0);
                }
                continue;
            }
            paragraphBuilder.append(trimmedLine).append(" ");

            boolean endsWithPunctuation = trimmedLine.endsWith(".") || trimmedLine.endsWith(":") || trimmedLine.endsWith(";");
            boolean nextLineIsNewSection = false;
            if (i + 1 < lines.length) {
                String nextLine = lines[i + 1].trim();
                if (nextLine.startsWith("Art.") || nextLine.startsWith("§") || nextLine.startsWith("Inc.")) {
                    nextLineIsNewSection = true;
                }
            }

            if (endsWithPunctuation || nextLineIsNewSection) {
                if (paragraphBuilder.length() > 0) {
                    rawParagraphs.add(paragraphBuilder.toString().trim());
                    paragraphBuilder.setLength(0);
                }
            }
        }
        if (paragraphBuilder.length() > 0) {
            rawParagraphs.add(paragraphBuilder.toString().trim());
        }

        StringBuilder htmlResult = new StringBuilder();

        for (String paragraph : rawParagraphs) {
            String cleanedParagraph = paragraph.trim();
            if (!cleanedParagraph.isEmpty()) {
                htmlResult.append("<p>").append(cleanedParagraph).append("</p>");
            }
        }

        return htmlResult.toString();
    }
}

