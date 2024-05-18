
import com.crawldata.back_end.model.Author;
import com.crawldata.back_end.model.Chapter;
import com.crawldata.back_end.model.Novel;
import com.crawldata.back_end.plugin_builder.PluginFactory;
import com.crawldata.back_end.response.DataResponse;
import com.crawldata.back_end.utils.ConnectJsoup;
import com.crawldata.back_end.utils.HandleString;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MeTruyenChuPlugin implements PluginFactory {
    private final String FILTER_NOVEL_API = "https://backend.metruyencv.com/api/books?filter[keyword]=%s&filter[state]=published&limit=100&page=1&include=author";
    private final String NOVEL_LIST_CHAPTERS_API = "https://backend.metruyencv.com/api/chapters?filter[book_id]=%s";
    private final String NOVEL_DETAIL_API = "https://backend.metruyencv.com/api/books/%s";
    private final String AUTHOR_DETAIL_API = "https://backend.metruyencv.com/api/books?filter[author]=%s&include=author&limit=100&page=1&filter[state]=published";
    private final String ALL_NOVELS_API = "https://backend.metruyencv.com/api/books?include=author&sort=-view_count&limit=20&page=%s&filter[state]=published";
    private final String NOVEL_SEARCH_API = "https://backend.metruyencv.com/api/books/search?keyword=%s&limit=20&page=%s&sort=-view_count&filter[state]=published";
    private final String CHAPTER_DETAIL_API = "https://metruyencv.com/truyen/%s/%s";

    /**
     * Reverses a slug string by replacing dashes and spaces with URL-encoded spaces.
     *
     * @param slug The slug string to reverse.
     * @return The reversed slug string.
     */
    private String reverseSlugging(String slug) {
        return slug.replaceAll("-", "%20").replaceAll(" ", "%20");
    }

    /**
     * Connects to the given API URL and returns the JSON response as a JsonObject.
     *
     * @param apiUrl The URL of the API to connect to.
     * @return The JSON response as a JsonObject, or null if an error occurs.
     */
    private JsonObject connectAPI(String apiUrl) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            // Make the GET request
            HttpGet request = new HttpGet(apiUrl);
            CloseableHttpResponse response = httpClient.execute(request);

            try {
                // Get the response entity
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // Convert the response to a String
                    String result = EntityUtils.toString(entity);

                    // Parse the JSON response using Gson
                    Gson gson = new Gson();
                    // Parse the JSON response
                    JsonObject jsonObject = null;
                    try {
                        jsonObject = gson.fromJson(result, JsonObject.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return jsonObject;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                response.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Retrieves the novel details by its slug.
     *
     * @param slug The slug of the novel.
     * @return The JsonObject representing the novel details.
     */
    private JsonObject getNovelDetailBySlug(String slug) {
        String apiUrl = String.format(FILTER_NOVEL_API, reverseSlugging(slug));

        JsonObject jsonObject = connectAPI(apiUrl);
        if (jsonObject!=null && jsonObject!=null && jsonObject!=null && jsonObject.has("data")) {
            JsonArray dataArray = jsonObject.getAsJsonArray("data");

            // Loop through the data array to find the matching slug
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject novel = dataArray.get(i).getAsJsonObject();
                if (novel.get("slug").getAsString().equals(slug)) {
                    return novel;
                }
            }
        }
        return null;
    }

    /**
     * Creates a Novel object from the given JsonObject.
     *
     * @param novelObject The JsonObject representing the novel data.
     * @return The created Novel object.
     */
    public Novel createNovelByJsonData(JsonObject novelObject) {
        String name = novelObject.get("name").getAsString();
        String novelId = novelObject.get("slug").getAsString();
        JsonObject authorObject = novelObject.getAsJsonObject("author");
        Author author = new Author(authorObject.get("id").getAsString()+"-"+ HandleString.makeSlug(authorObject.get("name").getAsString()), authorObject.get("name").getAsString());
        String idFirstChapter = "chuong-1";
        String image = novelObject.getAsJsonObject("poster").get("default").getAsString();
        String description = novelObject.get("synopsis").getAsString().replaceAll("\n", "<br>");
        return new Novel(novelId, name, image, description, author, idFirstChapter);
    }

    /**
     * Creates a Chapter object from the given JsonObject and Novel object.
     *
     * @param chapterObject The JsonObject representing the chapter data.
     * @param novel The Novel object to which the chapter belongs.
     * @return The created Chapter object.
     */
    public Chapter createChapterByJsonData(JsonObject chapterObject, Novel novel) {
        String name = chapterObject.get("name").getAsString();
        String chapterId = "chuong-" + chapterObject.get("index").getAsString();
        return new Chapter(novel.getNovelId(), novel.getName(), chapterId, null, null, name, novel.getAuthor(), "");
    }

    @Override
    public DataResponse getNovelChapterDetail(String novelId, String chapterId) {
        String urlChapter = String.format(CHAPTER_DETAIL_API, novelId, chapterId);
        JsonObject novelObject = getNovelDetailBySlug(novelId);
        assert novelObject != null;
        Novel novel = createNovelByJsonData(novelObject);
        Document doc = null;
        try {
            doc = ConnectJsoup.connect(urlChapter);

            // Extract the script tag containing JSON data
            Element scriptElement = doc.select("script:containsData(window.chapterData)").first();
            String scriptContent = scriptElement.html();

            // Extract JSON data from script content
            String nextData = scriptContent.substring(
                    scriptContent.indexOf("\"next\":") + " \"next\":".length() - 1,
                    scriptContent.indexOf("}", scriptContent.indexOf("\"next\":{")) + 1
            );
            Gson gson = new Gson();
            JsonObject nextObject = gson.fromJson(nextData, JsonObject.class);
            // Extract JSON data from script content
            String previousData = scriptContent.substring(
                    scriptContent.indexOf("\"previous\":") + " \"previous\":".length() - 1,
                    scriptContent.indexOf("}", scriptContent.indexOf("\"previous\":{")) + 1
            );
            JsonObject previousObject = gson.fromJson(previousData, JsonObject.class);
            Integer nextIndex = nextObject.get("index")!=null?nextObject.get("index").getAsInt():null;
            Integer previousIndex = previousObject.get("index")!=null?previousObject.get("index").getAsInt():null;

            String nextChapterId = nextIndex!=null?"chuong-"+nextIndex:null;
            String preChapterId = previousIndex!=null?"chuong-"+previousIndex:null;

            // Extract the chapter name
            String chapterName = doc.select("h2.text-center.text-gray-600.text-balance").first().text();

            // Extract the chapter content
            Element contentElement = doc.select("div[data-x-bind='ChapterContent']").first();
            String content = contentElement.html();

            // Create chapter details
            Chapter chapterDetail = new Chapter(novelId, novel.getName(), chapterId, nextChapterId, preChapterId, chapterName, novel.getAuthor(), content);

            // Prepare response
            DataResponse dataResponse = new DataResponse();
            dataResponse.setStatus("success");
            dataResponse.setData(chapterDetail);

            return dataResponse;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DataResponse getNovelListChapters(String novelId, int page) {
        JsonObject novelObject = getNovelDetailBySlug(novelId);
        assert novelObject != null;
        Novel novel = createNovelByJsonData(novelObject);
        String apiUrl = String.format(NOVEL_LIST_CHAPTERS_API, novelObject.get("id").getAsString());
        JsonObject jsonObject = connectAPI(apiUrl);
        List<Chapter> chapterList = new ArrayList<>();

        final int limit = 30; // items per page
        int totalPages = 1;
        int totalItems = 0;

        if (jsonObject != null && jsonObject.has("data")) {
            JsonArray dataArray = jsonObject.getAsJsonArray("data");
            totalItems = dataArray.size();
            totalPages = (int) Math.ceil((double) totalItems / limit);

            // Calculate the start and end indices for the requested page
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, totalItems);

            // Loop through the data and get the subset for the requested page
            for (int i = startIndex; i < endIndex; i++) {
                JsonObject chapterObject = dataArray.get(i).getAsJsonObject();
                chapterList.add(createChapterByJsonData(chapterObject, novel));
            }
        }

        return new DataResponse("success", totalPages, page, chapterList.size(), null, chapterList, "");
    }

    @Override
    public DataResponse getNovelDetail(String novelId) {
        JsonObject novelObject = getNovelDetailBySlug(novelId);
        if (novelObject != null) {
            Novel novel = createNovelByJsonData(novelObject);
            DataResponse dataResponse = new DataResponse();
            dataResponse.setData(novel);
            dataResponse.setStatus("success");
            return dataResponse;
        } else {
            return new DataResponse("error", null, null, null, null, null, "Can't get novel detail");
        }
    }

    @Override
    public DataResponse getAuthorDetail(String authorId) {
        String apiUrl = String.format(AUTHOR_DETAIL_API, authorId.split("-")[0]);
        JsonObject jsonObject = connectAPI(apiUrl);
        List<Novel> novelList = new ArrayList<>();
        assert jsonObject != null;
        if (jsonObject!=null && jsonObject.has("data")) {
            JsonArray dataArray = jsonObject.getAsJsonArray("data");
            // Loop through the data
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject novelObject = dataArray.get(i).getAsJsonObject();
                novelList.add(createNovelByJsonData(novelObject));
            }
        }
        return new DataResponse("success", 1, 1, novelList.size(), null, novelList, null);
    }

    @Override
    public DataResponse getAllNovels(int page, String search) {
        String apiUrl = String.format(ALL_NOVELS_API, page);
        JsonObject jsonObject = connectAPI(apiUrl);
        List<Novel> novelList = new ArrayList<>();
        assert jsonObject != null;
        if (jsonObject!=null && jsonObject.has("data")) {
            JsonArray dataArray = jsonObject.getAsJsonArray("data");
            // Loop through the data
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject novelObject = dataArray.get(i).getAsJsonObject();
                novelList.add(createNovelByJsonData(novelObject));
            }
        } else {
            DataResponse dataResponse = new DataResponse();
            dataResponse.setStatus("error");
            return dataResponse;
        }
        int totalPages = jsonObject.getAsJsonObject("pagination").get("last").getAsInt();
        return new DataResponse("success", totalPages, page, novelList.size(), search, novelList, "");
    }

    @Override
    public DataResponse getNovelSearch(int page, String key, String orderBy) {
        String apiUrl = String.format(NOVEL_SEARCH_API, reverseSlugging(key), page);
        JsonObject jsonObject = connectAPI(apiUrl);
        List<Novel> novelList = new ArrayList<>();
        assert jsonObject != null;
        if (jsonObject!=null && jsonObject.has("data")) {
            JsonArray dataArray = jsonObject.getAsJsonArray("data");
            // Loop through the data
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject novelObject = dataArray.get(i).getAsJsonObject();
                novelList.add(createNovelByJsonData(novelObject));
            }
        }
        int totalPages = jsonObject.getAsJsonObject("pagination").get("last").getAsInt();
        return new DataResponse("success", totalPages, page, novelList.size(), key, novelList, "");
    }
}
