package com.resumeai.service;

import com.resumeai.dto.CreateTemplateRequest;
import com.resumeai.dto.ResumeTemplateDetail;
import com.resumeai.dto.ResumeTemplateItem;
import com.resumeai.dto.ResumeTemplateResponse;
import com.resumeai.model.ResumeTemplate;
import com.resumeai.repository.ResumeTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 简历模板服务
 */
@Service
public class ResumeTemplateService {

    private final ResumeTemplateRepository resumeTemplateRepository;

    public ResumeTemplateService(ResumeTemplateRepository resumeTemplateRepository) {
        this.resumeTemplateRepository = resumeTemplateRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaultTemplates() {
        if (resumeTemplateRepository.count() > 0) {
            return;
        }

        createDefaultTemplate(
                "Java后端工程师模板",
                "互联网",
                "Java后端工程师",
                """
                # 张三
                - 目标岗位：Java后端工程师
                - 联系方式：138****1234 | zhangsan@example.com
                - GitHub: github.com/zhangsan

                ## 职业摘要
                5年Java后端开发经验，熟悉Spring Boot、微服务架构、分布式系统设计。擅长高并发系统优化、数据库性能调优。

                ## 核心技能
                - 编程语言：Java、Python、SQL
                - 框架技术：Spring Boot、Spring Cloud、MyBatis、Dubbo
                - 数据库：MySQL、Redis、MongoDB、Elasticsearch
                - 中间件：Kafka、RabbitMQ、Zookeeper
                - 工具链：Docker、Kubernetes、Jenkins、Git

                ## 工作经历
                ### XX科技有限公司 | 高级Java工程师 | 2021.06 - 至今
                - S：负责电商平台核心交易系统，日均订单量100万+，高峰期QPS达5000+
                - T：优化系统性能，降低响应时间，提升系统稳定性
                - A：引入Redis缓存、消息队列削峰、数据库读写分离，重构核心下单流程
                - R：系统响应时间从800ms降至150ms，订单处理能力提升3倍，故障率下降85%

                ### YY互联网公司 | Java工程师 | 2019.07 - 2021.05
                - S：参与用户中心微服务改造，单体应用拆分为10+微服务
                - T：负责用户认证、权限管理模块设计与开发
                - A：基于Spring Cloud Gateway实现统一网关，使用JWT实现无状态认证
                - R：系统可用性提升至99.95%，支持日活用户从50万增长至200万

                ## 项目经历
                ### 分布式订单系统
                - 技术栈：Spring Boot、MySQL、Redis、Kafka、Elasticsearch
                - 职责：核心开发，负责订单状态机设计、分布式事务处理、实时数据同步
                - 成果：支持每秒5000+订单创建，数据一致性达99.99%

                ### 用户画像系统
                - 技术栈：Flink、Kafka、HBase、Elasticsearch
                - 职责：实时计算引擎开发，用户行为数据ETL，标签体系设计
                - 成果：实时处理10亿+用户行为事件，标签更新延迟<5秒

                ## 教育背景
                - XX大学 | 计算机科学与技术 | 本科 | 2015.09 - 2019.06

                ## 证书与荣誉
                - 阿里云ACP认证
                - 公司年度优秀员工（2022）
                """,
                "5年Java后端经验，熟悉Spring Boot、微服务、高并发系统优化"
        );

        createDefaultTemplate(
                "前端工程师模板",
                "互联网",
                "前端工程师",
                """
                # 李四
                - 目标岗位：前端工程师
                - 联系方式：139****5678 | lisi@example.com
                - 个人网站：lisi.dev

                ## 职业摘要
                4年前端开发经验，精通Vue、React生态，擅长组件化开发、性能优化、工程化建设。有大型ToB/ToC项目经验。

                ## 核心技能
                - 前端框架：Vue 3、React 18、TypeScript
                - 状态管理：Vuex、Pinia、Redux、Zustand
                - 构建工具：Vite、Webpack、Rollup
                - UI框架：Element Plus、Ant Design、Tailwind CSS
                - 工程化：ESLint、Prettier、Husky、CI/CD

                ## 工作经历
                ### AA科技公司 | 高级前端工程师 | 2022.03 - 至今
                - S：负责企业级SaaS平台前端架构设计，支持10+业务模块
                - T：提升开发效率，统一技术栈，优化首屏加载性能
                - A：搭建Vue 3 + TypeScript微前端架构，封装20+业务组件库，引入Vite构建
                - R：首屏加载时间从3.5s降至1.2s，开发效率提升40%，代码复用率提升60%

                ### BB互联网公司 | 前端工程师 | 2020.07 - 2022.02
                - S：参与电商小程序开发，日活用户50万+
                - T：负责商品详情页、购物车、订单流程开发
                - A：使用uni-app跨端开发，封装通用组件，优化图片加载策略
                - R：页面加载速度提升50%，转化率提升15%

                ## 项目经历
                ### 企业级数据可视化平台
                - 技术栈：Vue 3、TypeScript、ECharts、WebSocket
                - 职责：前端架构设计，图表组件封装，实时数据渲染优化
                - 成果：支持100+图表类型，实时刷新延迟<200ms，支持10万+数据点渲染

                ### 低代码表单设计器
                - 技术栈：React、Ant Design、JSON Schema
                - 职责：拖拽编辑器开发，表单渲染引擎，校验规则引擎
                - 成果：支持30+表单组件，配置化生成表单，减少80%重复开发

                ## 教育背景
                - YY大学 | 软件工程 | 本科 | 2016.09 - 2020.06

                ## 开源贡献
                - Vue官方文档中文翻译贡献者
                - 维护开源组件库vue-awesome-components（500+ stars）
                """,
                "4年前端经验，精通Vue/React，擅长组件化、性能优化、工程化"
        );

        createDefaultTemplate(
                "产品经理模板",
                "互联网",
                "产品经理",
                """
                # 王五
                - 目标岗位：产品经理
                - 联系方式：136****9012 | wangwu@example.com
                - LinkedIn: linkedin.com/in/wangwu

                ## 职业摘要
                5年产品经验，擅长ToB SaaS产品设计、用户体验优化、数据驱动决策。成功主导3款0-1产品上线，累计服务企业客户1000+。

                ## 核心能力
                - 产品规划：需求分析、竞品分析、产品路线图、MVP设计
                - 用户研究：用户访谈、可用性测试、A/B测试、数据分析
                - 项目管理：敏捷开发、Scrum、需求评审、版本管理
                - 工具技能：Axure、Figma、Jira、SQL、Google Analytics

                ## 工作经历
                ### CC科技公司 | 高级产品经理 | 2021.08 - 至今
                - S：负责企业协作SaaS产品，面向中小企业客户，竞争激烈
                - T：提升产品竞争力，增加付费转化率，降低客户流失率
                - A：主导产品重构，优化核心工作流，新增智能推荐、自动化工作流等功能
                - R：付费转化率从8%提升至15%，客户留存率提升25%，NPS从45提升至68

                ### DD互联网公司 | 产品经理 | 2019.07 - 2021.07
                - S：负责在线教育平台课程管理模块，用户反馈课程发现效率低
                - T：优化课程推荐算法，提升用户学习时长
                - A：设计个性化推荐系统，引入标签体系，优化搜索排序策略
                - R：用户日均学习时长提升40%，课程完成率提升30%

                ## 项目经历
                ### 企业知识管理系统（0-1产品）
                - 背景：企业内部知识分散，查找效率低，知识沉淀困难
                - 方案：设计知识库、文档协作、智能搜索、权限管理等核心功能
                - 成果：上线6个月服务200+企业客户，续费率85%，获客户好评

                ### 移动端工作台改版
                - 背景：移动端使用率低，用户反馈操作复杂
                - 方案：重新设计信息架构，简化操作流程，优化关键路径
                - 成果：移动端DAU提升60%，任务完成效率提升50%

                ## 教育背景
                - ZZ大学 | 工商管理 | 本科 | 2015.09 - 2019.06

                ## 证书与培训
                - PMP项目管理认证
                - 产品经理实战训练营（人人都是产品经理）
                """,
                "5年产品经验，擅长ToB SaaS、用户体验、数据驱动，主导3款0-1产品"
        );

        createDefaultTemplate(
                "数据分析师模板",
                "互联网",
                "数据分析师",
                """
                # 赵六
                - 目标岗位：数据分析师
                - 联系方式：137****3456 | zhaoliu@example.com

                ## 职业摘要
                4年数据分析经验，精通SQL、Python数据分析，擅长业务指标体系搭建、用户行为分析、A/B测试。有电商、金融行业经验。

                ## 核心技能
                - 数据分析：SQL、Python（Pandas、NumPy）、Excel、SPSS
                - 数据可视化：Tableau、Power BI、ECharts、Matplotlib
                - 统计方法：假设检验、回归分析、聚类分析、时间序列
                - 业务能力：指标体系设计、用户画像、漏斗分析、留存分析

                ## 工作经历
                ### EE科技公司 | 高级数据分析师 | 2022.05 - 至今
                - S：负责电商平台数据分析，业务增长遇到瓶颈
                - T：识别增长机会，优化转化漏斗，提升GMV
                - A：搭建用户行为分析体系，识别高价值用户特征，设计精准营销策略
                - R：GMV季度增长35%，营销ROI提升50%，用户复购率提升20%

                ### FF金融公司 | 数据分析师 | 2020.07 - 2022.04
                - S：负责风控模型优化，坏账率偏高
                - T：降低坏账率，提升风控准确性
                - A：分析历史数据，构建用户信用评分模型，优化审批规则
                - R：坏账率下降40%，审批通过率提升15%，风控准确率提升至92%

                ## 项目经历
                ### 用户增长分析体系搭建
                - 背景：缺乏统一的增长指标体系，数据分散
                - 方案：设计AARRR模型，搭建数据看板，建立周报机制
                - 成果：识别3个关键增长点，推动产品优化，月活提升25%

                ### A/B测试平台建设
                - 背景：产品迭代缺乏数据验证，决策依赖主观判断
                - 方案：搭建A/B测试流程，设计实验方案，分析实验结果
                - 成果：完成20+次A/B测试，验证功能效果，避免3次错误决策

                ## 教育背景
                - PP大学 | 统计学 | 硕士 | 2018.09 - 2020.06
                - QQ大学 | 数学与应用数学 | 本科 | 2014.09 - 2018.06

                ## 证书
                - CDA数据分析师认证（Level II）
                """,
                "4年数据分析经验，精通SQL/Python，擅长指标体系、用户分析、A/B测试"
        );

        createDefaultTemplate(
                "运维工程师模板",
                "互联网",
                "运维工程师",
                """
                # 孙七
                - 目标岗位：运维工程师
                - 联系方式：135****7890 | sunqi@example.com

                ## 职业摘要
                5年运维经验，熟悉Linux系统管理、容器化部署、CI/CD流程、监控告警体系。擅长自动化运维、故障排查、性能优化。

                ## 核心技能
                - 操作系统：Linux（CentOS、Ubuntu）、Shell脚本
                - 容器编排：Docker、Kubernetes、Helm
                - CI/CD：Jenkins、GitLab CI、ArgoCD
                - 监控告警：Prometheus、Grafana、ELK、Zabbix
                - 云平台：阿里云、AWS、腾讯云
                - 自动化：Ansible、Terraform

                ## 工作经历
                ### GG科技公司 | 高级运维工程师 | 2021.09 - 至今
                - S：负责公司核心业务系统运维，服务器200+台，日均请求10亿+
                - T：提升系统稳定性，降低故障率，优化部署效率
                - A：搭建K8s集群，实现容器化部署，建立完善的监控告警体系，推动自动化运维
                - R：系统可用性从99.5%提升至99.95%，部署效率提升80%，故障响应时间从30分钟降至5分钟

                ### HH互联网公司 | 运维工程师 | 2019.07 - 2021.08
                - S：负责Web应用运维，频繁出现性能问题
                - T：优化系统性能，提升用户体验
                - A：分析性能瓶颈，优化Nginx配置，引入Redis缓存，调整数据库索引
                - R：响应时间从2s降至300ms，并发处理能力提升5倍

                ## 项目经历
                ### Kubernetes集群建设
                - 背景：传统虚拟机部署效率低，资源利用率不足
                - 方案：搭建K8s集群，实现服务容器化，配置自动扩缩容
                - 成果：部署时间从30分钟降至3分钟，资源利用率提升40%

                ### 监控告警体系建设
                - 背景：缺乏统一监控，故障发现滞后
                - 方案：部署Prometheus+Grafana，配置告警规则，接入钉钉/企业微信
                - 成果：故障发现时间从平均20分钟降至1分钟，建立100+监控指标

                ## 教育背景
                - RR大学 | 计算机科学与技术 | 本科 | 2015.09 - 2019.06

                ## 证书
                - CKA（Certified Kubernetes Administrator）
                - 阿里云ACP认证
                """,
                "5年运维经验，熟悉Linux、K8s、CI/CD、监控告警，擅长自动化运维"
        );

        createDefaultTemplate(
                "测试工程师模板",
                "互联网",
                "测试工程师",
                """
                # 周八
                - 目标岗位：测试工程师
                - 联系方式：138****2345 | zhouba@example.com

                ## 职业摘要
                4年测试经验，熟悉功能测试、接口测试、性能测试、自动化测试。擅长测试用例设计、缺陷管理、测试流程优化。

                ## 核心技能
                - 测试类型：功能测试、接口测试、性能测试、安全测试
                - 自动化测试：Selenium、Appium、Pytest、JUnit
                - 接口测试：Postman、JMeter、RestAssured
                - 性能测试：JMeter、Locust、Gatling
                - 测试管理：Jira、TestRail、禅道
                - 编程语言：Python、Java

                ## 工作经历
                ### II科技公司 | 高级测试工程师 | 2022.06 - 至今
                - S：负责电商平台测试，手工测试占比高，回归测试耗时长
                - T：提升测试效率，降低线上缺陷率
                - A：搭建自动化测试框架，编写1000+自动化用例，建立CI/CD集成测试流程
                - R：回归测试时间从2天降至2小时，测试覆盖率提升至85%，线上缺陷率下降60%

                ### JJ互联网公司 | 测试工程师 | 2020.07 - 2022.05
                - S：负责移动App测试，兼容性问题频发
                - T：提升App质量，减少兼容性问题
                - A：建立设备兼容性测试矩阵，设计专项测试用例，引入云测试平台
                - R：兼容性问题下降70%，用户评分从3.8提升至4.5

                ## 项目经历
                ### 自动化测试框架搭建
                - 背景：手工测试效率低，回归测试成本高
                - 方案：基于Pytest+Selenium搭建Web自动化框架，支持数据驱动、关键字驱动
                - 成果：编写800+自动化用例，覆盖核心业务流程，节省60%回归测试时间

                ### 性能测试体系建设
                - 背景：缺乏性能测试，上线后频繁出现性能问题
                - 方案：使用JMeter设计性能测试场景，建立性能基线，定期压测
                - 成果：识别5个性能瓶颈，推动优化，系统并发能力提升3倍

                ## 教育背景
                - SS大学 | 软件工程 | 本科 | 2016.09 - 2020.06

                ## 证书
                - ISTQB软件测试工程师认证
                """,
                "4年测试经验，熟悉功能/接口/性能测试，擅长自动化测试、测试框架搭建"
        );
    }

    private void createDefaultTemplate(String name, String industry, String targetRole, String content, String previewText) {
        ResumeTemplate template = new ResumeTemplate();
        template.setName(name);
        template.setIndustry(industry);
        template.setTargetRole(targetRole);
        template.setContent(content);
        template.setPreviewText(previewText);
        template.setUsageCount(0);
        template.setCreatedAt(Instant.now());
        resumeTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public ResumeTemplateResponse listTemplates(String industry, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ResumeTemplate> pageResult;

        String cleanIndustry = clean(industry);
        if (cleanIndustry.isEmpty()) {
            pageResult = resumeTemplateRepository.findAllByOrderByUsageCountDesc(pageable);
        } else {
            pageResult = resumeTemplateRepository.findByIndustryOrderByUsageCountDesc(cleanIndustry, pageable);
        }

        List<ResumeTemplateItem> items = new ArrayList<>();
        for (ResumeTemplate template : pageResult.getContent()) {
            ResumeTemplateItem item = new ResumeTemplateItem();
            item.setId(template.getId());
            item.setName(template.getName());
            item.setIndustry(template.getIndustry());
            item.setTargetRole(template.getTargetRole());
            item.setPreviewText(template.getPreviewText());
            item.setUsageCount(template.getUsageCount());
            item.setCreatedAt(template.getCreatedAt());
            items.add(item);
        }

        ResumeTemplateResponse response = new ResumeTemplateResponse();
        response.setItems(items);
        response.setPage(page);
        response.setSize(size);
        response.setTotal(pageResult.getTotalElements());
        return response;
    }

    @Transactional
    public ResumeTemplateDetail getTemplate(Long id) {
        ResumeTemplate template = resumeTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("template not found"));

        // 增加使用次数
        template.setUsageCount(template.getUsageCount() + 1);
        resumeTemplateRepository.save(template);

        ResumeTemplateDetail detail = new ResumeTemplateDetail();
        detail.setId(template.getId());
        detail.setName(template.getName());
        detail.setIndustry(template.getIndustry());
        detail.setTargetRole(template.getTargetRole());
        detail.setContent(template.getContent());
        detail.setPreviewText(template.getPreviewText());
        detail.setUsageCount(template.getUsageCount());
        detail.setCreatedAt(template.getCreatedAt());
        return detail;
    }

    @Transactional
    public ResumeTemplateDetail createTemplate(CreateTemplateRequest request) {
        String name = clean(request.getName());
        String content = clean(request.getContent());

        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }

        ResumeTemplate template = new ResumeTemplate();
        template.setName(name);
        template.setIndustry(clean(request.getIndustry()));
        template.setTargetRole(clean(request.getTargetRole()));
        template.setContent(content);
        template.setPreviewText(clean(request.getPreviewText()));
        template.setUsageCount(0);
        template.setCreatedAt(Instant.now());

        template = resumeTemplateRepository.save(template);

        ResumeTemplateDetail detail = new ResumeTemplateDetail();
        detail.setId(template.getId());
        detail.setName(template.getName());
        detail.setIndustry(template.getIndustry());
        detail.setTargetRole(template.getTargetRole());
        detail.setContent(template.getContent());
        detail.setPreviewText(template.getPreviewText());
        detail.setUsageCount(template.getUsageCount());
        detail.setCreatedAt(template.getCreatedAt());
        return detail;
    }

    @Transactional
    public ResumeTemplateDetail updateTemplate(Long id, CreateTemplateRequest request) {
        ResumeTemplate template = resumeTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("template not found"));

        String name = clean(request.getName());
        String content = clean(request.getContent());

        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }

        template.setName(name);
        template.setIndustry(clean(request.getIndustry()));
        template.setTargetRole(clean(request.getTargetRole()));
        template.setContent(content);
        template.setPreviewText(clean(request.getPreviewText()));

        template = resumeTemplateRepository.save(template);

        ResumeTemplateDetail detail = new ResumeTemplateDetail();
        detail.setId(template.getId());
        detail.setName(template.getName());
        detail.setIndustry(template.getIndustry());
        detail.setTargetRole(template.getTargetRole());
        detail.setContent(template.getContent());
        detail.setPreviewText(template.getPreviewText());
        detail.setUsageCount(template.getUsageCount());
        detail.setCreatedAt(template.getCreatedAt());
        return detail;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!resumeTemplateRepository.existsById(id)) {
            throw new IllegalArgumentException("template not found");
        }
        resumeTemplateRepository.deleteById(id);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
