package googol;

import java.io.Serializable;
import java.util.*;

/**
 * Representa uma página web com URL, título, links, contagem de links referenciados,
 * lista de links referenciados e palavras de conteúdo.
 * 
 * <p>Esta classe permite a manipulação dos atributos da página, como adicionar links e
 * obter informações sobre os conteúdos e referências da página.</p>
 * 
 * @author 
 */
public class Page implements Serializable {

    private static final long serialVersionUID = 1L;

    private String url;
    private String pageTitle;
    private List<String> links;
    private int referedLinksCount;
    private List<String> referedLinks;
    private List<String> contentWords;
    private String text;

    /**
     * Construtor da classe Page.
     *
     * @param url           o URL da página
     * @param pageTitle     o título da página
     * @param links         a lista de links referenciados (referedLinks)
     * @param contentWords  a lista de palavras presentes no conteúdo da página
     */
    public Page(String url, String pageTitle, List<String> links, List<String> contentWords, String text) {
        this.url = url;
        this.pageTitle = pageTitle;
        this.referedLinks = links;
        this.referedLinksCount = 0;
        this.contentWords = contentWords;
        this.text = text;

        this.links = new ArrayList<String>();
    }

    /**
     * Adiciona um novo link à lista de links.
     *
     * @param link o link a ser adicionado
     */
    public void addLink(String link) {
        links.add(link);
    }

    /**
     * Retorna a lista de palavras que compõem o conteúdo da página.
     *
     * @return a lista de palavras de conteúdo
     */
    public List<String> getContentWords() {
        return contentWords;
    }

    public String getText() {
        return text;
    }

    /**
     * Retorna o URL da página.
     *
     * @return o URL da página
     */
    public String getUrl() {
        return url;
    }

    /**
     * Retorna a contagem de links referenciados.
     *
     * @return a contagem de links referenciados
     */
    public int getReferedLinksCount() {
        return referedLinksCount;
    }

    /**
     * Retorna a lista de links referenciados.
     *
     * @return a lista de links referenciados
     */
    public List<String> getReferedLinks() {
        return referedLinks;
    }

    /**
     * Incrementa a contagem de links referenciados em um.
     */
    public void incrementReferedLinksCount() {
        referedLinksCount++;
    }

    /**
     * Verifica se a página atual é igual à página passada como parâmetro, com base na URL.
     *
     * @param page a página a ser comparada
     * @return true se as URLs forem iguais; false caso contrário
     */
    public boolean isEquals(Page page) {
        return this.url.equals(page.getUrl());
    }

    /**
     * Define a contagem de links referenciados.
     *
     * @param referedLinksCount a nova contagem de links referenciados
     */
    public void setReferedLinksCount(int referedLinksCount) {
        this.referedLinksCount = referedLinksCount;
    }

    /**
     * Atualiza os atributos da página.
     *
     * @param url           o nova URL da página
     * @param pageTitle     o novo título da página
     * @param links         a nova lista de links referenciados
     * @param contentWords  a nova lista de palavras do conteúdo da página
     */
    public void setPage(String url, String pageTitle, List<String> links, List<String> contentWords, String text) {
        this.url = url;
        this.pageTitle = pageTitle;
        this.referedLinks = links;
        this.contentWords = contentWords;
        this.text = text;
    }

    /**
     * Retorna o título da página.
     *
     * @return o título da página
     */
    public String getPageTitle() {
        return pageTitle;
    }

    /**
     * Retorna a lista de links adicionados.
     *
     * @return a lista de links
     */
    public List<String> getLinks() {
        return links;
    }

}
