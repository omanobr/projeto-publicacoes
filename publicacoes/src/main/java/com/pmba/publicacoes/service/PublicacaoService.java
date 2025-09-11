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
    private String getTextFromHeaderArea(PDPage page) throws IOException {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        // Define uma pequena área no topo da página para verificar se há um cabeçalho.
        // Usamos uma altura de 80, que é a sua margem máxima atual.
        Rectangle2D.Float headerRegion = new Rectangle2D.Float(0, 0, page.getMediaBox().getWidth(), 80);
        stripper.addRegion("header", headerRegion);
        stripper.extractRegions(page);
        return stripper.getTextForRegion("header").trim();
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

    private String processarVinculosParaEdicao(Publicacao publicacao) {
        String conteudoOriginal = publicacao.getConteudoHtml();

        // Filtra apenas os vínculos de revogação parcial gerados por esta publicação
        List<VinculoNormativo> vinculosDeRevogacao = publicacao.getVinculosGerados().stream()
                .filter(v -> v.getTipoVinculo() == TipoVinculo.REVOGA_PARCIALMENTE)
                .toList();

        if (vinculosDeRevogacao.isEmpty()) {
            return conteudoOriginal; // Retorna o original se não houver revogações parciais
        }

        // Usamos o Jsoup para manipular o HTML de forma segura
        Document doc = Jsoup.parseBodyFragment(conteudoOriginal);

        for (VinculoNormativo vinculo : vinculosDeRevogacao) {
            String textoParaRevogar = vinculo.getTextoDoTrecho();
            if (textoParaRevogar != null && !textoParaRevogar.isEmpty()) {
                // Encontra todos os elementos que contêm o texto a ser revogado
                doc.body().getElementsContainingOwnText(textoParaRevogar).forEach(element -> {
                    String originalHtml = element.html();
                    // Envolve o texto exato com a tag <s> para o efeito de taxado
                    String novoHtml = originalHtml.replace(textoParaRevogar, "<s>" + textoParaRevogar + "</s>");
                    element.html(novoHtml);
                });
            }
        }

        return doc.body().html();
    }
    @Transactional(readOnly = true)
    public PublicacaoEditDTO findByIdForEditing(Long id) {
        Publicacao publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publicação não encontrada com id: " + id));

        Hibernate.initialize(publicacao.getVinculosGerados());

        // Carrega os vínculos que esta publicação RECEBE para processamento visual
        List<VinculoNormativo> vinculosRecebidosEntities = vinculoRepository.findAllByPublicacaoDestinoId(id);

        // VVV--- LÓGICA DE PROCESSAMENTO VISUAL PARA O EDITOR ---VVV
        String conteudoParaEdicao = publicacao.getConteudoHtml();
        Document doc = Jsoup.parseBodyFragment(conteudoParaEdicao);

        for (VinculoNormativo vinculo : vinculosRecebidosEntities) {
            if (vinculo.getTipoVinculo() == TipoVinculo.REVOGA_PARCIALMENTE) {
                String textoRevogado = vinculo.getTextoDoTrecho();
                if (textoRevogado != null && !textoRevogado.isEmpty()) {
                    // Envolve o texto com uma tag <s> para o efeito de taxado.
                    // O editor TipTap reconhecerá e estilizará isso.
                    doc.body().html(doc.body().html().replace(textoRevogado, "<s>" + textoRevogado + "</s>"));
                }
            } else if (vinculo.getTipoVinculo() == TipoVinculo.ALTERA) {
                String textoAlterado = vinculo.getTextoNovo();
                if (textoAlterado != null && !textoAlterado.isEmpty()) {
                    // Envolve o texto com uma tag <u> para o efeito de sublinhado.
                    doc.body().html(doc.body().html().replace(textoAlterado, "<u>" + textoAlterado + "</u>"));
                }
            }
        }
        conteudoParaEdicao = doc.body().html();
        // ^^^--- FIM DA LÓGICA DE PROCESSAMENTO ---^^^

        PublicacaoEditDTO dto = new PublicacaoEditDTO();
        dto.setId(publicacao.getId());
        dto.setTitulo(publicacao.getTitulo());
        dto.setNumero(publicacao.getNumero());
        dto.setTipo(publicacao.getTipo());
        dto.setDataPublicacao(publicacao.getDataPublicacao());
        dto.setStatus(publicacao.getStatus());
        dto.setBgo(publicacao.getBgo());
        dto.setConteudoHtml(conteudoParaEdicao); // Usa o conteúdo processado

        List<VinculoSimpleDTO> vinculosGerados = publicacao.getVinculosGerados().stream()
                .map(this::convertVinculoToSimpleDto)
                .collect(Collectors.toList());
        dto.setVinculosGerados(vinculosGerados);

        List<VinculoSimpleDTO> vinculosRecebidos = vinculosRecebidosEntities.stream()
                .map(this::convertVinculoToSimpleDto)
                .collect(Collectors.toList());
        dto.setVinculosRecebidos(vinculosRecebidos);

        return dto;
    }

    private String processarVinculos(Publicacao publicacao) {
        String conteudoOriginal = publicacao.getConteudoHtml();
        Document doc = Jsoup.parseBodyFragment(conteudoOriginal);
        List<VinculoNormativo> vinculosRecebidos = vinculoRepository.findAllByPublicacaoDestinoId(publicacao.getId());

        // VVV--- LÓGICA DE ANOTAÇÃO APRIMORADA ---VVV
        // Primeiro, processa as alterações, que são as mais complexas
        for (VinculoNormativo vinculo : vinculosRecebidos) {
            if (vinculo.getTipoVinculo() == TipoVinculo.ALTERA) {
                // Procura pela tag <ins> específica com o ID do vínculo
                Element insElement = doc.selectFirst("ins.alteracao[data-vinculo-id=" + vinculo.getId() + "]");
                if (insElement != null) {
                    String tipoOrigem = vinculo.getPublicacaoOrigem().getTipo();
                    String tipoFormatado = tipoOrigem.substring(0, 1).toUpperCase() + tipoOrigem.substring(1).toLowerCase();

                    String anotacaoHtml = String.format(
                            "<div class=\"anotacao-alteracao\">" +
                                    "Alterado pela %s <a href=\"/publicacao/%d\">%s</a>.<br>" +
                                    "Redação original: \"%s\"" +
                                    "</div>",
                            tipoFormatado,
                            vinculo.getPublicacaoOrigem().getId(),
                            vinculo.getPublicacaoOrigem().getNumero(),
                            vinculo.getTextoDoTrecho()
                    );

                    // Insere a anotação após o parágrafo pai da tag <ins>
                    Element parentParagraph = insElement.closest("p");
                    if (parentParagraph != null) {
                        parentParagraph.after(anotacaoHtml);
                    } else {
                        insElement.after(anotacaoHtml); // Fallback caso não esteja em um <p>
                    }
                }
            }
        }

        // Processa os outros vínculos depois, para evitar problemas com substituições de texto
        String htmlProcessado = doc.body().html();
        for (VinculoNormativo vinculo : vinculosRecebidos) {
            String tipoOrigem = vinculo.getPublicacaoOrigem().getTipo();
            String tipoFormatado = tipoOrigem.substring(0, 1).toUpperCase() + tipoOrigem.substring(1).toLowerCase();

            if (vinculo.getTipoVinculo() == TipoVinculo.REVOGA_PARCIALMENTE) {
                String textoOriginal = vinculo.getTextoDoTrecho();
                if (textoOriginal != null && !textoOriginal.isEmpty()) {
                    String linkRevogacao = String.format(
                            "<a href=\"/publicacao/%d\" class=\"trecho-revogado\" data-vinculo-info=\"Revogado pela %s %s\">" +
                                    "<del>%s</del>" +
                                    "</a>",
                            vinculo.getPublicacaoOrigem().getId(), tipoFormatado, vinculo.getPublicacaoOrigem().getNumero(), textoOriginal);
                    htmlProcessado = htmlProcessado.replace(textoOriginal, linkRevogacao);
                }
            } else if (vinculo.getTipoVinculo() == TipoVinculo.ACRESCENTA) {
                String textoDeReferencia = vinculo.getTextoDoTrecho();
                String textoNovo = vinculo.getTextoNovo();
                // Precisamos do Jsoup novamente para inserções complexas
                Document docTemp = Jsoup.parseBodyFragment(htmlProcessado);
                Element p = docTemp.select("p:containsOwn(" + textoDeReferencia + ")").first();
                if (p != null) {
                    String textoAcrescentadoHtml = String.format("<p class=\"paragrafo-acrescentado\"><ins>%s</ins></p>", textoNovo);
                    String anotacaoHtml = String.format("<div class=\"anotacao-vinculo-info\"><small>(Acrescentado pela %s <a href=\"/publicacao/%d\">%s</a>)</small></div>",
                            tipoFormatado, vinculo.getPublicacaoOrigem().getId(), vinculo.getPublicacaoOrigem().getNumero());
                    p.after(anotacaoHtml);
                    p.after(textoAcrescentadoHtml);
                    htmlProcessado = docTemp.body().html();
                }
            }
        }
        // ^^^--- FIM DA LÓGICA APRIMORADA ---^^^

        return htmlProcessado;
    }

    @Transactional(readOnly = true)
    public List<PublicacaoListDTO> searchPublicacoes(BuscaPublicacaoDTO buscaDTO) {
        Specification<Publicacao> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lógica para o campo específico de número (usado na HomePage)
            if (buscaDTO.getNumero() != null && !buscaDTO.getNumero().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("numero")), "%" + buscaDTO.getNumero().toLowerCase() + "%"));
            }

            // VVV--- LÓGICA ALTERADA ---VVV
            // Lógica para o campo de termo geral (usado no Modal e na HomePage)
            if (buscaDTO.getTermo() != null && !buscaDTO.getTermo().isEmpty()) {
                String termoBusca = "%" + buscaDTO.getTermo().toLowerCase() + "%";

                // Cria as condições de busca para cada campo
                Predicate tituloLike = cb.like(cb.lower(root.get("titulo")), termoBusca);
                Predicate numeroLike = cb.like(cb.lower(root.get("numero")), termoBusca); // <-- BUSCA NO NÚMERO

                // Adiciona uma única condição OR que combina a busca nos três campos
                predicates.add(cb.or(tituloLike, numeroLike));
            }
            // ^^^--- FIM DA ALTERAÇÃO ---^^^

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

            // VVV--- LÓGICA DE DETECÇÃO DINÂMICA DE CABEÇALHO ---VVV
            boolean hasHeader = false;
            if (document.getNumberOfPages() > 1) {
                String headerPage1 = getTextFromHeaderArea(document.getPage(0));
                String headerPage2 = getTextFromHeaderArea(document.getPage(1));
                // Considera que há um cabeçalho se o texto na área superior das duas
                // primeiras páginas for idêntico e não estiver vazio.
                if (!headerPage1.isEmpty() && headerPage1.equals(headerPage2)) {
                    hasHeader = true;
                }
            }

            // Define a margem superior com base na presença de um cabeçalho
            float margemSuperior = hasHeader ? 80.0f : 40.0f;
            float margemInferior = 40.0f;
            // ^^^--- FIM DA LÓGICA DE DETECÇÃO ---^^^

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();

                // Usa a margem superior dinâmica
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

