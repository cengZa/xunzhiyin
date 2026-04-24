package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.domain.vo.DemoCandidateSpotlightVO;
import com.lcj.campusreco.domain.vo.DemoStoryVO;
import com.lcj.campusreco.service.DemoStoryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DemoStoryServiceImpl implements DemoStoryService {

    @Override
    public DemoStoryVO getDefaultStory(String scenarioMode) {
        String normalizedMode = RecommendationScenarioMode.normalize(scenarioMode);
        DemoStoryVO story = new DemoStoryVO();
        story.setDemoUserId(2001L);
        story.setScenarioMode(normalizedMode);
        story.setScenarioLabel(RecommendationScenarioMode.labelOf(normalizedMode));
        story.setPersonaSummary(
                "2001 号用户是计算机方向学生，偏好 Java、Spring、音乐、ACM 和羽毛球，目标是在校园里找到既能聊兴趣、又适合线下建立连接的同学。"
        );
        switch (normalizedMode) {
            case RecommendationScenarioMode.STUDY_PARTNER -> buildStudyStory(story);
            case RecommendationScenarioMode.CLUB_PARTNER -> buildClubStory(story);
            default -> buildInterestStory(story);
        }
        return story;
    }

    private void buildStudyStory(DemoStoryVO story) {
        story.setStoryTitle("演示用户 2001：学习搭子模式");
        story.setStoryNarrative("该模式优先强调专业方向、学术标签和年级接近度，适合展示“兴趣画像 + 学习场景重排”的算法效果。");
        story.getAlgorithmHighlights().addAll(List.of(
                "优先看 Java、Spring、ACM、机器学习等学习标签的贡献",
                "同专业或相近专业的候选人会被明显前移",
                "资料不完整、标签稀疏的候选人会被可信连接分抑制"
        ));
        story.getExpectedCandidateIds().addAll(List.of(2002L, 2011L, 2013L));
        story.getCandidateSpotlights().add(createSpotlight(
                2002L,
                "柏宇",
                "与 2001 同为计算机方向，Java、Spring、ACM 和 AI 的重合度最高，是学习搭子模式的第一候选。",
                List.of("Java", "Spring", "ACM", "AI")
        ));
        story.getCandidateSpotlights().add(createSpotlight(
                2011L,
                "乔溪",
                "更偏全栈与 ACM，适合展示高质量技术型候选人在学习场景中的稳定排序。",
                List.of("Java", "Spring", "ACM", "MachineLearning")
        ));
        story.getCandidateSpotlights().add(createSpotlight(
                2013L,
                "苏衡",
                "偏 AI、数据挖掘和辩论，适合展示“技术重合 + 非同专业但可协作”的学习搭子候选。",
                List.of("AI", "MachineLearning", "DataMining")
        ));
    }

    private void buildClubStory(DemoStoryVO story) {
        story.setStoryTitle("演示用户 2001：社团搭子模式");
        story.setStoryNarrative("该模式优先强调 ACM、学生组织、志愿活动和运动类标签，适合展示“校园场景重排”如何改变默认排序。");
        story.getAlgorithmHighlights().addAll(List.of(
                "社团和校园活动标签的命中权重更高",
                "年级接近和线下参与便利性会影响排序",
                "热门标签重合但资料稀疏的候选人不会轻易排到最前"
        ));
        story.getExpectedCandidateIds().addAll(List.of(2014L, 2009L, 2010L));
        story.getCandidateSpotlights().add(createSpotlight(
                2014L,
                "唐澈",
                "学生会、志愿活动、辩论和运动标签齐全，是社团搭子模式下最适合展示的典型候选。",
                List.of("Volunteering", "StudentUnion", "Debate", "Badminton")
        ));
        story.getCandidateSpotlights().add(createSpotlight(
                2009L,
                "罗栀",
                "偏校园活动运营和创意传播，适合展示跨专业但校园活动连接强的社团候选人。",
                List.of("Music", "Photography", "Volunteering", "StudentUnion")
        ));
        story.getCandidateSpotlights().add(createSpotlight(
                2010L,
                "孟夏",
                "创业活动和学生组织参与度高，适合展示“活动组织能力”在社团模式中的加权。",
                List.of("Startup", "StudentUnion", "Debate")
        ));
    }

    private void buildInterestStory(DemoStoryVO story) {
        story.setStoryTitle("演示用户 2001：兴趣同频模式");
        story.setStoryNarrative("该模式优先强调兴趣相似，再用校园规则和可信连接分做轻量校正，适合用作首页默认演示模式。");
        story.getAlgorithmHighlights().addAll(List.of(
                "先看兴趣画像相似度，再做校园场景和可信连接校正",
                "跨专业但兴趣高度重合的候选人会被保留下来",
                "解释会直接展示匹配标签、命中规则和可信原因"
        ));
        story.getExpectedCandidateIds().addAll(List.of(2015L, 2003L, 2006L));
        story.getCandidateSpotlights().add(createSpotlight(
                2015L,
                "周霁",
                "音乐、摄影、吉他和羽毛球同时命中，是兴趣同频模式下最适合讲解的一号候选。",
                List.of("Music", "Photography", "Guitar", "Badminton")
        ));
        story.getCandidateSpotlights().add(createSpotlight(
                2003L,
                "陈栀",
                "偏摄影、音乐和前端设计，适合展示跨专业兴趣匹配而不是专业同类匹配。",
                List.of("Music", "Photography", "Frontend")
        ));
        story.getCandidateSpotlights().add(createSpotlight(
                2006L,
                "何川",
                "音乐和运动兴趣更突出，适合展示纯兴趣型候选人在默认模式中的位置。",
                List.of("Music", "Badminton", "Guitar")
        ));
    }

    private DemoCandidateSpotlightVO createSpotlight(Long candidateUserId,
                                                     String candidateNickname,
                                                     String storyReason,
                                                     List<String> highlightTags) {
        DemoCandidateSpotlightVO spotlight = new DemoCandidateSpotlightVO();
        spotlight.setCandidateUserId(candidateUserId);
        spotlight.setCandidateNickname(candidateNickname);
        spotlight.setStoryReason(storyReason);
        spotlight.getHighlightTags().addAll(highlightTags);
        return spotlight;
    }
}
