package com.crawldata.demo.plugin_builder.tangthuvien;
import com.crawldata.back_end.model.Author;
import com.crawldata.back_end.model.Chapter;
import com.crawldata.back_end.model.Novel;
import com.crawldata.back_end.novel_plugin_builder.tangthuvien.TangThuVienPlugin;
import com.crawldata.back_end.response.DataResponse;
import com.crawldata.back_end.utils.ConnectJsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TangThuVienPluginTest {
    private final String ROOT_URL = "https://truyen.tangthuvien.vn/";
    private final String  NOVEL_DETAIL_URL = ROOT_URL + "/doc-truyen/%s";
    @InjectMocks
    private TangThuVienPlugin tangThuVienPlugin;

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.openMocks(this);

    }

    @Test
    public void testGetAuthorIdFromUrl() {
        String url = "https://truyen.tangthuvien.vn/tac-gia?author=123";
        String authorId = tangThuVienPlugin.getAuthorIdFromUrl(url);
        assertEquals("123", authorId);
    }

    @Test
    public void testGetNovelIdFromUrl() {
        String url = "https://truyen.tangthuvien.vn/doc-truyen/456";
        String novelId = tangThuVienPlugin.getNovelIdFromUrl(url);
        assertEquals("456", novelId);
    }

    @Test
    public void testGetTotalChapterFromText() {
        String text = "Danh sách chương (100 chương)";
        Integer totalChapters = tangThuVienPlugin.getTotalChapterFromText(text);
        assertEquals(100, totalChapters);
    }

    @Test
    public void testGetChapterIdFromUrl() {
        String url = "https://truyen.tangthuvien.vn/doc-truyen/456/chapter-1";
        String chapterId = tangThuVienPlugin.getChapterIdFromUrl(url);
        assertEquals("chapter-1", chapterId);
    }

    @Test
    public void getNovelChapterDetail_invalidNovelId_error() throws IOException {
        String novelId = "invalid-novel-id";
        String chapterId = "valid-chapter-id";
        DataResponse result = tangThuVienPlugin.getNovelChapterDetail(novelId, chapterId);
        assertNotNull(result);
        assertEquals("error", result.getStatus());
    }
    @Test
    void testGetNovelListChaptersSuccess() throws IOException {
        String novelId = "novelId1";
        String chapterName = "name1";
        String novelName = "novel1";
        String storyId = "storyId1";
        String expectedStatus = "success";
        String fromChapter = "chuong-1";

        List<Chapter> chapters = new ArrayList<>();

        chapters.add(new Chapter().novelId(novelId).novelName(novelName).chapterId(fromChapter).name(chapterName).author(new Author("john-wick","john wick")));
        int numChapter = 1;

        Map<String,Object> map = new HashMap<>();
        map.put("novelName",novelName);
        map.put("author", chapters.get(0).getAuthor());
        map.put("storyId", storyId);

        doReturn(map).when(tangThuVienPlugin).mapNovelInfo(novelId);
        doReturn(chapters).when(tangThuVienPlugin).getAllChaptersImpl(storyId);

        // Calling the method under test
        DataResponse response = tangThuVienPlugin.getNovelListChapters(novelId, fromChapter, numChapter);
        // Assertions
        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatus());
        assertNotNull(response.getData());
        List<Chapter> chaptersRS = (List<Chapter>) response.getData();
        assertEquals(1, chaptersRS.size());
    }
    @Test
    void testGetNovelListChaptersError() throws IOException {
        String novelId = "Invalid novel id";
        String chapterName = "name1";
        String novelName = "novel1";
        String storyId = "storyId1";
        String expectedStatus = "error";
        String fromChapter = "chuong-1";

        List<Chapter> chapters = new ArrayList<>();

        chapters.add(new Chapter().novelId(novelId).novelName(novelName).chapterId(fromChapter).name(chapterName).author(new Author("john-wick","john wick")));
        int numChapter = 1;


        doReturn(null).when(tangThuVienPlugin).mapNovelInfo(novelId);
        doReturn(chapters).when(tangThuVienPlugin).getAllChaptersImpl(storyId);

        // Calling the method under test
        DataResponse response = tangThuVienPlugin.getNovelListChapters(novelId, fromChapter, numChapter);
        // Assertions
        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatus());
    }
    @Test
    void testGetAllNovelListChaptersSuccess() {
        String novelId = "dai-dao-ky";

        String expectedStatus = "error";
        String fromChapter = "chuong-1";
        int numChapter = 10;

        // Calling the method under test
        DataResponse response = tangThuVienPlugin.getNovelListChapters(novelId, fromChapter, numChapter);
        // Assertions
        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatus());
    }
    @Test
    void testGetAllNovelListChaptersError() {
        String novelId = "dai-dao-ky";

        String expectedStatus = "error";
        String fromChapter = "chuong-1";
        int numChapter = 10;

        // Calling the method under test
        DataResponse response = tangThuVienPlugin.getNovelListChapters(novelId, fromChapter, numChapter);
        // Assertions
        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatus());
    }
    @Test
    void getNovelsPerPageSuccess()
    {
        String expectedStatus = "success";
        int currentPage = 1;
        int totalPage = 599;
        int perPage = 20;
        int size = 20;
        DataResponse dataResponse = tangThuVienPlugin.getAllNovels(1, "");
        List<Novel> novels = (List<Novel>) dataResponse.getData();
        // Assertions
        assertNotNull(dataResponse);
        assertEquals(expectedStatus,dataResponse.getStatus());
        assertEquals(currentPage, dataResponse.getCurrentPage());
        assertEquals(totalPage, dataResponse.getTotalPage());
        assertEquals(perPage, dataResponse.getPerPage());
        assertEquals(size, novels.size());
    }

    @Test
    void getNovelsPerPageError()
    {
        String expectedStatus = "error";
        DataResponse dataResponse = tangThuVienPlugin.getAllNovels(600, "");
        // Assertions
        assertNotNull(dataResponse);
        assertEquals(expectedStatus,dataResponse.getStatus());
    }

    @Test
    void getNovelSearchSuccess()
    {
        String keyword = "kiếm";
        int page = 1;
        String orderBy = "a-z";
        DataResponse dataResponse = tangThuVienPlugin.getNovelSearch(page, keyword, orderBy);
        List<Novel> novels = (List<Novel>) dataResponse.getData();
        String expectedStatus = "success";
        int currentPage = 1;
        int totalPage = 27;
        String searchValue = "kiếm";
        int size = 20;
        // Assertions
        assertNotNull(dataResponse);
        assertEquals(expectedStatus,dataResponse.getStatus());
        assertEquals(totalPage, dataResponse.getTotalPage());
        assertEquals(currentPage, dataResponse.getCurrentPage());
        assertEquals(searchValue, dataResponse.getSearchValue());
        assertEquals(size, novels.size());
    }

    @Test
    void getNovelSearchError1()
    {
        String keyword = "kiếm";
        int page = 1000;
        String orderBy = "a-z";
        DataResponse dataResponse = tangThuVienPlugin.getNovelSearch(page, keyword, orderBy);
        String expectedStatus = "error";
        // Assertions
        assertNotNull(dataResponse);
        assertEquals(expectedStatus,dataResponse.getStatus());
    }

    @Test
    void getNovelSearchError2()
    {
        String keyword = "zzzzzzzzzzzzzzzzzz";
        int page = 1000;
        String orderBy = "a-z";
        DataResponse dataResponse = tangThuVienPlugin.getNovelSearch(page, keyword, orderBy);
        String expectedStatus = "error";
        // Assertions
        assertNotNull(dataResponse);
        assertEquals(expectedStatus,dataResponse.getStatus());
    }


}