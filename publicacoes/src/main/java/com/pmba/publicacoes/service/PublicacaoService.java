package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.*;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.StatusPublicacao;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
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
        return convertToDetailDto(salvo);
    }

    @Transactional(readOnly = true)
    public PublicacaoDetailDTO findByIdProcessado(Long id) {
        Publicacao publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicação não encontrada com id: " + id));
        PublicacaoDetailDTO dto = convertToDetailDto(publicacao);

        List<VinculoNormativo> vinculosRecebidos = vinculoRepository.findAllByPublicacaoDestinoId(id);
        List<VinculoNormativo> vinculosGerados = vinculoRepository.findAllByPublicacaoOrigemId(id);

        Set<VinculoPublicacaoInfoDTO> publicacoesVinculadas = new HashSet<>();

        for (VinculoNormativo vinculo : vinculosRecebidos) {
            publicacoesVinculadas.add(new VinculoPublicacaoInfoDTO(vinculo.getPublicacaoOrigem().getId(), vinculo.getPublicacaoOrigem().getTitulo()));
        }
        for (VinculoNormativo vinculo : vinculosGerados) {
            publicacoesVinculadas.add(new VinculoPublicacaoInfoDTO(vinculo.getPublicacaoDestino().getId(), vinculo.getPublicacaoDestino().getTitulo()));
        }

        dto.setPublicacoesVinculadas(new ArrayList<>(publicacoesVinculadas));

        String conteudoProcessado = processarVinculos(publicacao);
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
        dto.setBgo(publicacao.getBgo());
        dto.setConteudoHtml(publicacao.getConteudoHtml());
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

    private String processarVinculos(Publicacao publicacao) {
        String conteudoOriginal = publicacao.getConteudoHtml();
        Document doc = Jsoup.parse(conteudoOriginal);

        List<VinculoNormativo> vinculosRecebidos = vinculoRepository.findAllByPublicacaoDestinoId(publicacao.getId());

        for (VinculoNormativo vinculo : vinculosRecebidos) {
            if (vinculo.getTipoVinculo() == TipoVinculo.ALTERA && vinculo.getTextoNovo() != null) {

                List<Element> elements = doc.select(":containsOwn(" + vinculo.getTextoNovo() + ")");
                Element p = elements.stream()
                        .filter(el -> el.tagName().equals("p"))
                        .findFirst()
                        .orElse(null);

                if (p != null) {
                    String anotacaoHtml = String.format(
                            "<div class=\"anotacao-alteracao\">" +
                                    "Alterado pela Publicação <a href=\"/publicacao/%d\">%s</a>.<br>" +
                                    "Redação original: \"%s\"" +
                                    "</div>",
                            vinculo.getPublicacaoOrigem().getId(),
                            vinculo.getPublicacaoOrigem().getNumero(),
                            vinculo.getTextoDoTrecho()
                    );
                    p.after(anotacaoHtml);
                }
            } else if (vinculo.getTipoVinculo() == TipoVinculo.REVOGA_PARCIALMENTE) {
                String textoOriginal = vinculo.getTextoDoTrecho();
                if (textoOriginal != null && !textoOriginal.isEmpty()) {
                    String linkRevogacao = String.format(
                            "<a href=\"/publicacao/%d\" class=\"trecho-revogado\" data-vinculo-info=\"Revogado pela Publicação %s\">" +
                                    "<del>%s</del>" +
                                    "</a>",
                            vinculo.getPublicacaoOrigem().getId(),
                            vinculo.getPublicacaoOrigem().getNumero(),
                            textoOriginal
                    );
                    doc.body().html(doc.body().html().replace(textoOriginal, linkRevogacao));
                }
            }
        }
        return doc.body().html();
    }

    @Transactional(readOnly = true)
    public List<PublicacaoListDTO> searchPublicacoes(BuscaPublicacaoDTO buscaDTO) {
        Specification<Publicacao> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (buscaDTO.getNumero() != null && !buscaDTO.getNumero().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("numero")), "%" + buscaDTO.getNumero().toLowerCase() + "%"));
            }
            if (buscaDTO.getTermo() != null && !buscaDTO.getTermo().isEmpty()) {
                Predicate tituloLike = cb.like(cb.lower(root.get("titulo")), "%" + buscaDTO.getTermo().toLowerCase() + "%");
                Predicate conteudoLike = cb.like(cb.lower(root.get("conteudoHtml")), "%" + buscaDTO.getTermo().toLowerCase() + "%");
                predicates.add(cb.or(tituloLike, conteudoLike));
            }
            if (buscaDTO.getAno() != null) {
                predicates.add(cb.equal(cb.function("date_part", Integer.class, cb.literal("year"), root.get("dataPublicacao")), buscaDTO.getAno()));
            }
            if (buscaDTO.getDataInicial() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataPublicacao"), buscaDTO.getDataInicial()));
            }
            if (buscaDTO.getDataFinal() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataPublicacao"), buscaDTO.getDataFinal()));
            }
            if (buscaDTO.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), buscaDTO.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Publicacao> publicacoes = publicacaoRepository.findAll(spec);

        if (publicacoes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> publicacaoIds = publicacoes.stream().map(Publicacao::getId).collect(Collectors.toList());
        Set<Long> idsPublicacoesAlteradas = vinculoRepository.findAlteredDestinoIds(publicacaoIds, Set.of(TipoVinculo.ALTERA, TipoVinculo.REVOGA_PARCIALMENTE));

        return publicacoes.stream()
                .map(pub -> {
                    PublicacaoListDTO dto = convertToListDto(pub);
                    if (dto.getStatus() == StatusPublicacao.ATIVA && idsPublicacoesAlteradas.contains(pub.getId())) {
                        dto.setFoiAlterada(true);
                    }
                    return dto;
                })
                .sorted(Comparator.comparing(PublicacaoListDTO::getDataPublicacao).reversed())
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

    private PublicacaoDetailDTO convertToDetailDto(Publicacao publicacao) {
        PublicacaoDetailDTO dto = new PublicacaoDetailDTO();
        dto.setId(publicacao.getId());
        dto.setTitulo(publicacao.getTitulo());
        dto.setNumero(publicacao.getNumero());
        dto.setTipo(publicacao.getTipo());
        dto.setDataPublicacao(publicacao.getDataPublicacao());
        dto.setConteudoHtml(publicacao.getConteudoHtml());
        dto.setStatus(publicacao.getStatus());
        dto.setBgo(publicacao.getBgo());
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

    public String extrairTextoDeArquivo(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String textoSimples;

        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
            StringBuilder textoCompleto = new StringBuilder();
            PDDocument document = PDDocument.load(file.getInputStream());
            float margemSuperior = 80;
            float margemInferior = 40;

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();

                float alturaRegiao = height - margemSuperior - margemInferior;

                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                Rectangle2D.Float region = new Rectangle2D.Float(0, margemSuperior, width, alturaRegiao);
                stripper.addRegion("content", region);
                stripper.extractRegions(page);
                textoCompleto.append(stripper.getTextForRegion("content"));
            }
            textoSimples = textoCompleto.toString();
            document.close();

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

            boolean endsWithPunctuation = trimmedLine.endsWith(".") || trimmedLine.endsWith(":") || trimmedLine.endsWith(";");
            boolean nextLineIsNewSection = false;
            if (i + 1 < lines.length) {
                String nextLine = lines[i + 1].trim();
                if (nextLine.startsWith("Art.") || nextLine.startsWith("§") || nextLine.startsWith("Inc.")) {
                    nextLineIsNewSection = true;
                }
            }

            paragraphBuilder.append(trimmedLine).append(" ");

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
            if (!paragraph.isEmpty()) {
                htmlResult.append("<p>").append(paragraph).append("</p>");
            }
        }

        return htmlResult.toString();
    }
}

