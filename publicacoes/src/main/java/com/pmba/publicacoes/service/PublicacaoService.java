package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.*;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.TipoVinculo;
import com.pmba.publicacoes.model.VinculoNormativo;
import com.pmba.publicacoes.repository.PublicacaoRepository;
import com.pmba.publicacoes.repository.VinculoNormativoRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import java.io.IOException;
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
        dto.setBgo(publicacao.getBgo());
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
        return publicacaoRepository.findAllForListView();
    }

    private String processarVinculos(Publicacao publicacao, boolean apenasTachado) {
        List<VinculoNormativo> vinculosRecebidos = vinculoRepository.findAllByPublicacaoDestinoId(publicacao.getId());
        String conteudoProcessado = publicacao.getConteudoHtml();

        if (vinculosRecebidos.isEmpty()) {
            return conteudoProcessado;
        }

        for (var vinculo : vinculosRecebidos) {
            String textoOriginal = vinculo.getTextoDoTrecho();
            if (textoOriginal == null || textoOriginal.isEmpty()) continue;
            String textoSubstituto;

            if (vinculo.getTipoVinculo() == TipoVinculo.ALTERA && vinculo.getTextoNovo() != null && !apenasTachado) {

                String textoNovoInline = vinculo.getTextoNovo()
                        .trim()
                        .replaceAll("</p>\\s*<p>", "<br>")
                        .replaceAll("^<p>", "")
                        .replaceAll("</p>$", "");
                textoSubstituto = String.format(
                        "<a href=\"/publicacao/%d\" class=\"trecho-alterado\" data-vinculo-info=\"Redação alterada pela Publicação %s\">" +
                                "<del>%s</del><br><span class=\"novo-texto\"> %s</span>" +
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

    // VVV--- MÉTODO DE BUSCA ATUALIZADO ---VVV
    @Transactional(readOnly = true)
    public List<PublicacaoListDTO> searchPublicacoes(String termo) {
        List<Publicacao> publicacoes;

        try {
            // Tenta converter o termo para uma data. O frontend já envia no formato AAAA-MM-DD.
            LocalDate data = LocalDate.parse(termo);
            // Se a conversão for bem-sucedida, busca por data usando o método do repositório.
            publicacoes = publicacaoRepository.findByDataPublicacao(data);
        } catch (DateTimeParseException e) {
            // Se a conversão falhar, significa que não é uma data, então busca por texto.
            publicacoes = publicacaoRepository.findByTermo(termo);
        }

        // Converte a lista de entidades (Publicacao) para a lista de DTOs (PublicacaoListDTO).
        return publicacoes.stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
    }
    // ^^^--- FIM DA ATUALIZAÇÃO ---^^^


    @Transactional(readOnly = true)
    public List<PublicacaoListDTO> searchPublicacoesAvancado(String conteudo) {
        List<Publicacao> publicacoes = publicacaoRepository.searchByConteudo(conteudo);
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
            PDDocument document = PDDocument.load(file.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            textoSimples = stripper.getText(document);
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
                continue;
            }
            paragraphBuilder.append(trimmedLine);

            boolean endsWithPunctuation = trimmedLine.endsWith(".") || trimmedLine.endsWith(":") || trimmedLine.endsWith(";");
            boolean isAllCaps = !trimmedLine.isEmpty() && trimmedLine.equals(trimmedLine.toUpperCase()) && trimmedLine.matches(".*[A-Z].*");
            boolean nextLineIsNewSection = false;
            if (i + 1 < lines.length) {
                String nextLine = lines[i + 1].trim();
                if (nextLine.startsWith("Art.") || nextLine.startsWith("§") || nextLine.startsWith("Inc.")) {
                    nextLineIsNewSection = true;
                }
            }

            if (endsWithPunctuation || isAllCaps || nextLineIsNewSection) {
                rawParagraphs.add(paragraphBuilder.toString());
                paragraphBuilder.setLength(0);
            } else {
                paragraphBuilder.append(" ");
            }
        }
        if (paragraphBuilder.length() > 0) {
            rawParagraphs.add(paragraphBuilder.toString().trim());
        }

        StringBuilder htmlResult = new StringBuilder();

        Pattern headerExclusionPattern = Pattern.compile(
                "\\d{1,2} de (Janeiro|Fevereiro|Março|Abril|Maio|Junho|Julho|Agosto|Setembro|Outubro|Novembro|Dezembro) de \\d{4}\\s+BGO\\s+Nº.*LEGISLAÇÃO",
                Pattern.CASE_INSENSITIVE
        );

        Pattern pageRemovalPattern = Pattern.compile("\\s*(Pág|Pagina|Pag)\\.?\\s*\\d{1,4}\\s*", Pattern.CASE_INSENSITIVE);

        for (String paragraph : rawParagraphs) {
            if (headerExclusionPattern.matcher(paragraph).find()) {
                continue;
            }

            String cleanedParagraph = pageRemovalPattern.matcher(paragraph).replaceAll("").trim();

            if (!cleanedParagraph.isEmpty()) {
                htmlResult.append("<p>").append(cleanedParagraph).append("</p>");
            }
        }

        return htmlResult.toString();
    }
}

