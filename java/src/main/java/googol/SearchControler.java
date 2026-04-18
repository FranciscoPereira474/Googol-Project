package googol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.rmi.RemoteException;
import java.util.List;


@Controller
public class SearchControler {

    private int page = 0;

    @PostMapping("/search")
    public String search(@RequestParam("url") String url, @RequestParam(defaultValue = "0") int page, Model model) {
        
        try {
            List<List<String>> results = null;
            // Lógica de busca com paginação
            results = gatewayService.searchWords(url, page); // Substitua pela lógica real de busca com paginação

            // Se a lista for nula, inicialize como uma lista vazia
            if (results == null) {
                results = List.of();
            }

            // Adiciona os resultados ao modelo
            model.addAttribute("results", results);
            model.addAttribute("page", page); // Adiciona o número da página ao modelo

        } catch (Exception e) {
            // Em caso de erro, exibe uma mensagem e inicializa a lista como vazia
            System.out.println("Erro ao buscar dados:");
        }

        return "searchResults"; // Retorna a página de resultados com o modelo atualizado
    }


    

    @Autowired
    private HackerNewsService hackerNewsService;

    @Autowired
    private GatewayService gatewayService;
    
    @GetMapping("/")
    public String index() throws InterruptedException {
        try {
            gatewayService.notifyClients("Welcome to the Search Engine!");
        } catch (RemoteException e) {
            System.out.println("Error while notifying clients: " + e.getMessage());
        }
        return "index"; // Nome do template HTML para a página inicial
    }

    

    @GetMapping("/searchWords") 
    public String searchPost() {
        return "searchResults"; // Nome do template HTML para exibir os resultados
    }

    

    @PostMapping("/search2")
    public String search2(@RequestParam("url") String url,@RequestParam(defaultValue = "0") int page, Model model) {
        try {
            // Simulação de lógica de busca
            List<String> results = gatewayService.searchLinks(url, page); // Substitua pela lógica real

            // Se a lista for nula, inicialize como uma lista vazia
            if (results == null) {
                results = List.of();
            }

            model.addAttribute("results", results); // Adiciona os resultados ao modelo
            model.addAttribute("page", page); // Adiciona o número da página ao modelo
        } catch (Exception e) {
            // Em caso de erro, exibe uma mensagem e inicializa a lista como vazia
            System.out.println("Erro aqui");
            e.printStackTrace();
            model.addAttribute("error", "An error occurred while searching: " + e.getMessage());
            model.addAttribute("results", List.of());
        }

        return "searchResults2"; // Sempre retorna a página de resultados
    }


    @GetMapping("/addUrl")
    public String addUrlForm() {
        return "addUrl"; // Nome do template HTML para o formulário de adição de URL
    }

    

    @PostMapping("/addUrlToQueue")
    public String addUrlSubmit(@RequestParam("url") String url, Model model) {
        if (url == null || url.trim().isEmpty()) {
        model.addAttribute("error", "Please provide a URL.");
        return "addUrl"; // Retorna à página com a mensagem de erro
    }

    // Validação do formato do URL
    String urlRegex = "^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$";
    if (!url.matches(urlRegex)) {
        model.addAttribute("error", "Invalid URL format. Please provide a valid URL.");
        return "addUrl"; // Retorna à página com a mensagem de erro
    }
        try {
            gatewayService.addUrl(url);
            model.addAttribute("message", "URL added successfully");
        } catch (RemoteException e) {
            model.addAttribute("error", "Error while adding URL: " + e.getMessage());
        }
        return "addUrl"; // Nome do template HTML para exibir a mensagem de sucesso ou erro
    }

    
    /*@GetMapping("/getStats")
    public String GetStats() {
        /*
        try{
            gatewayService.notifyClients("message");
        }catch (RemoteException e){
            System.out.println("Error while notifying clients: " + e.getMessage());
        }
        
        return "stats"; // Nome do template HTML para exibir as estatísticas
    }
    */

    @GetMapping("/resultados2")
    public String mostrarResultados2(@RequestParam("query") String query, Model model) {
        model.addAttribute("query", query);
        return "resultados2"; // corresponde a resultados.html em /templates
    }

    @GetMapping("/resultados")
    public String mostrarResultados(@RequestParam("query") String query, Model model) {
        model.addAttribute("query", query);
        return "resultados"; // corresponde a resultados.html em /templates
    }


    @GetMapping("/addHackerNewsTopStories")
    @ResponseBody
    public String addHackerNewsTopStories() {
        try {
            String search = "Escreve um poema sobre o mar.";
            hackerNewsService.indexTopLinks(search);
            return "Hacker News top stories indexed successfully.";
        } catch (Exception e) {
            return "Error indexing Hacker News top stories: " + e.getMessage();
        }
    }


    @Controller
    public class CustomErrorController implements ErrorController {

        @RequestMapping("/error")
        public String handleError(HttpServletRequest request, Model model) {
            Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

            if (status != null) {
                int statusCode = Integer.parseInt(status.toString());
                model.addAttribute("statusCode", statusCode);

                
            }
            return "generic-error"; 
        }
    }

    
}
