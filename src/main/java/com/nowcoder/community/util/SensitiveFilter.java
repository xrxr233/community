package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词过滤器：数据结构+算法
 * */
@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    /* 替换符号 */
    private static final String REPLACEMENT = "***";

    /* 根节点 */
    private TrieNode rootNode = new TrieNode();

    /* 初始化前缀树，在Bean被创建后调用该方法 */
    @PostConstruct
    public void init() {
        //加载.txt文件
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
            reader = new BufferedReader(new InputStreamReader(is));
            String keyword;

            //添加敏感词到前缀树
            while((keyword = reader.readLine()) != null) {
                this.addKeyword(keyword);
            }
        }catch (IOException e) {
            logger.error("加载敏感词文件失败：" + e.getMessage());
        }finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("关闭缓冲流失败：" + e.getMessage());
                }
            }
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("关闭流失败：" + e.getMessage());
                }
            }
        }
    }

    /* 将一个敏感词添加到前缀树 */
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        int len = keyword.length();
        for(int i = 0; i < len; i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if(subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            tempNode = subNode;
        }
        tempNode.setKeyWordEnd(true);
    }

    /* 检索敏感词，返回处理过后的文本 */
    public String filter(String text) {
        if(StringUtils.isBlank(text)) {
            return null;
        }
        //指针
        TrieNode tempNode = rootNode;
        int begin = 0;
        int end = 0;

        //记录结果
        StringBuilder sb = new StringBuilder();

        int len = text.length();
        boolean isKeyword = false;

        while(begin < len) {
            tempNode = rootNode;
            end = begin;
            isKeyword = false;
            while(end < len) {
                char c = text.charAt(end);
                //跳过特殊符号
                if(isSymbol(c)) {
                    end++;
                    continue;
                }
                TrieNode subNode = tempNode.getSubNode(c);
                if(subNode != null) {
                    if(subNode.isKeyWordEnd()) {
                        //是敏感词
                        isKeyword = true;
                        break;
                    }else {
                        //疑似敏感词，继续向下遍历
                        tempNode = subNode;
                        end++;
                    }
                }else {
                    //不是敏感词
                    break;
                }
            }
            if(isKeyword) {
                //text[begin, end]是敏感词，替换为***
                sb.append(REPLACEMENT);
                begin = end + 1;
            }else {
                //拼接text(begin)到StringBuilder
                sb.append(text.charAt(begin++));
            }
        }

        return sb.toString();
    }

    /* 判断是否为符号 */
    private boolean isSymbol(char c) {
        //isAsciiAlphanumeric判断是否是一般的文字
        //0x2E80-0x9FFF是东亚文字，不属于特殊符号
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * 前缀树节点
     * */
    private class TrieNode {
        //敏感词结束标识
        private boolean isKeyWordEnd = false;

        //当前节点的子节点（key是下级节点字符，value是下级节点）
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeyWordEnd() {
            return isKeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            isKeyWordEnd = keyWordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        //获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }
}
