package googol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hacker-news")
public class HackerNewsService {

    private static final Logger logger = LoggerFactory.getLogger(HackerNewsService.class);
    private static final String HACKER_NEWS_URL = "https://hacker-news.firebaseio.com/v0";
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private GatewayService gatewayService;

    @GetMapping("/index-top-links")
    public String indexTopLinks(@RequestParam String search) {
        String topStoriesURL = HACKER_NEWS_URL + "/topstories.json";
        Integer[] topStoryIds = restTemplate.getForObject(topStoriesURL, Integer[].class);

        if (topStoryIds == null || topStoryIds.length == 0) {
            return "No top stories found.";
        }

        String[] keywords = search.toLowerCase().split("\\s+");

        List<String> links = Arrays.stream(topStoryIds)
                .limit(100)
                .parallel()
                .map(id -> {
                    try {
                        String itemUrl = HACKER_NEWS_URL + "/item/" + id + ".json";
                        HackerNewsItemRecord item = restTemplate.getForObject(itemUrl, HackerNewsItemRecord.class);
                        if (item != null && item.title() != null && item.url() != null) {
                            boolean matches = Arrays.stream(keywords)
                                    .allMatch(word -> item.title().toLowerCase().contains(word));
                            if (matches) {
                                return item.url();
                            }
                        }
                    } catch (RestClientException e) {
                        logger.warn("Failed to fetch story with ID: {}", id, e);
                    }
                    return null;
                })
                .filter(link -> link != null)
                .limit(50)
                .collect(Collectors.toList());

        int indexed = 0;
        for (String url : links) {
            try {
                gatewayService.addUrl(url);
                indexed++;
            } catch (RemoteException e) {
                logger.error("Failed to index URL: {}", url, e);
            }
        }

        return "Indexed " + indexed + " link(s) containing: \"" + search + "\"";
    }
}


