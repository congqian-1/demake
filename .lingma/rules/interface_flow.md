---
trigger: model_decision
description: 当用户请求生成接口流程图、或提及“流程图”、“流程”、“Mermaid”等关键词时生效
---

# 🔥 **Interface Flowchart Generation Guidelines (Mermaid)**

> 用于在 Claude/ChatGPT 自动生成 Spring Boot 接口流程图时确保输出格式统一、逻辑清晰、包含数据库和 Redis 信息。

## 1. Mermaid Base Rules

请根据代码生成接口流程图（Mermaid flowchart TD），但必须遵守以下精简规范：

1. 只展示关键逻辑步骤，不展示类名、方法名、Wrapper 等细节。
2. 必须展示：
   - 数据库操作（标注真实表名）
   - Redis操作（标注真实 Key）
   - 第三方接口调用（标注 URL）
   - MQ操作（标注真实的topic和consumer group）
3. Service/Manager/Mapper 层级不需要详细展开，只需描述动作用途。
4. 分支判断只展示结果，不展示内部判断逻辑。
5. if,switch等逻辑判断，对于是和否需要两条分支呈现
6. 请注意：finally 块的执行时机是在 try 和 catch 块执行完毕之后、方法实际返回（或异常抛出）之前，即使其中有 return、throw 等控制转移语句。请基于这一原则解释其行为
7. 操作类型对应的流程图形状与颜色规范
   1. 开始/结束节点：椭圆形 ((...))，填充色为 #2E8B57（深绿），文字白色；
   2. 处理步骤：矩形 [...]，填充色为 #4682B4（钢蓝），文字白色；
   3. 判断分支：菱形 {...}，填充色为 #FFA500（橙色），文字黑色；
   4. 数据库操作：圆柱形 [(...)，填充色为 #FFD700（金色），文字黑色；
   5. Redis操作：圆角矩形 (...)，填充色为 #98FB98（浅绿），文字黑色，变量用$前缀；
   6. feignclient和http的第三方接口调用：平行四边形 [/.../]，填充色为 #FF6347（番茄红），文字黑色；
   7. MQ操作：六边形 {{...}}，填充色为 #DA70D6（兰花紫），文字黑色。
8. 注意:请在输出前自行验证生成的 Mermaid 流程图语法是否合法。若存在潜在解析错误（例如：节点文本中包含 `()`或`<`等 导致被误识别为特殊符号、未闭合的 HTML 标签、非法换行等），请主动调整措辞、替换括号（如用 `[ ]` 或中文描述代替 `( )`）、简化文本或重构节点内容，确保最终代码可被标准 Mermaid 渲染器（如 mermaid-js、GitLab、Typora）无错解析。
9. 全程保持流程图简洁，结构清晰。
10. 在流程图后面输出接口说明，格式参考下面输出的示例

---

## 2. Example Input

生成/api/v2/team/joinTeam的流程图

---

## 3. Example Output
接口流程图
```
flowchart TD
    Start((开始)) --> A[接收加入队伍请求]
    A --> B[获取用户信息<br>通过TokenUtils获取当前用户]
    B --> C[(查询活动信息<br>表: fun_activity_info)]
    C --> D{活动是否上架状态?}
    D -->|否| X[返回活动状态异常]
    D -->|是| E[/校验用户参与权限<br>调用launchAuthActivity方法/]
    E --> F{用户是否有权限?}
    F -->|否| Y[返回无权限异常]
    F -->|是| G{活动是否已满员?}
    G -->|是| Z[返回活动已满异常]
    G -->|否| H(获取分布式锁<br>Redis Key: join:team:$userId_$teamId)
    H --> I{是否获取到锁?}
    I -->|否| W[返回请求频率过快]
    I -->|是| J[(查询用户是否已加入该活动<br>表: fun_team_detail)]
    J --> K{用户是否已加入?}
  
    %% 修改点：已报名时，先释放锁，再返回异常
    K -->|是| V_Unlock(释放分布式锁)
    V_Unlock --> V[返回已报名异常]
  
    K -->|否| L[设置队伍成员信息<br>昵称、头像、积分等]
    L --> M[(添加课程收藏<br>表: edu_member_course_favorite)]
    M --> N[(更新活动参与人数<br>表: fun_activity_info)]
    N --> O[(添加学习记录<br>表: edu_course_view_history)]
    O --> P{是否有学习计划?}
    P -->|是| Q[(添加学习计划<br>表: edu_plan_detail)]
    P -->|否| R[(更新队伍成员数量<br>表: fun_team_info)]
    Q --> R
    R --> S{是否通过邀请加入?}
    S -->|是| T[(处理邀请逻辑<br>查询分享码信息<br>表: fun_share_code)]
    T --> U[(更新邀请人积分<br>表: fun_team_detail & fun_points_data)]
    U --> VV[(保存队伍成员信息<br>表: fun_team_detail)]
    S -->|否| VV
    VV --> WW(刷新队伍列表<br>Redis缓存更新)
    WW --> XX{{发送消息提醒<br>内部消息系统}}
    XX --> YY(释放分布式锁)
    YY --> ZZ[返回成功结果]

    X --> End((结束))
    Y --> End((结束))
    Z --> End((结束))
    W --> End((结束))
    V --> End((结束))
    ZZ --> End((结束))

    %% 样式
    classDef startEnd fill:#2E8B57,color:#fff;
    classDef process fill:#4682B4,color:#fff;
    classDef decision fill:#FFA500,color:#000;
    classDef db fill:#FFD700,color:#000;
    classDef redis fill:#98FB98,color:#000;
    classDef api fill:#FF6347,color:#000;
    classDef mq fill:#DA70D6,color:#000;

    class Start,End startEnd;
    class A,B,L,ZZ process;
    class D,F,G,I,K,P,S decision;
    class C,J,M,N,O,Q,R,T,U,VV db;
    class H,WW,YY,V_Unlock redis;
    class E api;
    class XX mq;
```
接口说明
1. 数据操作汇总
    1. 数据库表
        无
    2. Redis Key
        无
    3. 外部接口(feignclient或者http接口)
        无
    4. MQ Topic
        无
2. 复杂SQL分析
    - teamDateList
        - sql
        ```sql
                SELECT
            d.id,
            d.activity_id,
            d.team_id,
            d.team_member,
            (case when left(bm.id_card,1)='C' and bm.mobile!=''
            ...
        ```
        - 逻辑说明
            - 查询指定活动和用户的队伍详情信息
            - 关联查询用户基础信息表 base_member 获取用户昵称和头像
            - xxx