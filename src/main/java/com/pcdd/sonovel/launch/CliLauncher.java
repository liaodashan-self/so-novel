package com.pcdd.sonovel.launch;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import com.pcdd.sonovel.core.Crawler;
import com.pcdd.sonovel.model.*;
import com.pcdd.sonovel.parse.BookParser;
import com.pcdd.sonovel.parse.TocParser;
import com.pcdd.sonovel.util.ConfigUtils;
import com.pcdd.sonovel.util.SourceUtils;
import picocli.CommandLine;

import java.util.List;

import static org.fusesource.jansi.AnsiRenderer.render;

/**
 * 命令行界面 (Command Line Interface)
 *
 * @author pcdd
 * Created at 2025/7/10
 */
@CommandLine.Command(
        name = "SoNovel",
        mixinStandardHelpOptions = true,
        versionProvider = CliLauncher.VersionProvider.class
)
public class CliLauncher implements Runnable {

    @CommandLine.Option(names = {"--url", "-u"}, description = "书籍详情页链接 (非目录页)")
    String bookUrl;
    @CommandLine.Option(names = {"--ext", "-e"}, description = "下载格式，默认 epub", defaultValue = "epub")
    String extName;

    private static final AppConfig config = ConfigUtils.defaultConfig();

    /**
     * 全本下载
     */
    @Override
    public void run() {
        Console.log("""
                下载链接: {}
                下载格式: {}""", bookUrl, extName);

        Rule rule = SourceUtils.getSource(bookUrl);
        config.setSourceId(rule.getId());
        config.setExtName(extName);

        Book book = new BookParser(config).parse(bookUrl);
        SearchResult sr = SearchResult.builder()
                .url(book.getUrl())
                .bookName(book.getBookName())
                .author(book.getAuthor())
                .latestChapter(book.getLatestChapter())
                .lastUpdateTime(book.getLastUpdateTime())
                .build();
        Console.log("<== 正在解析目录...");
        TocParser tocParser = new TocParser(config);
        List<Chapter> toc = tocParser.parse(sr.getUrl());
        Console.log("<== 《{}》({})，共计 {} 章", sr.getBookName(), sr.getAuthor(), toc.size());
        double res = new Crawler(config).crawl(sr.getUrl(), toc);
        Console.log(render("<== 完成！总耗时 {} s", "green"), NumberUtil.round(res, 2));
    }

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{render(config.getVersion(), "green")};
        }
    }

}