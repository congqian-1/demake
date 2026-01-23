/*
 * Copyright (c) 2022 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tongzhou.mes.service1.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tongzhou.mes.service1.form.ApplicationForm;
import com.tongzhou.mes.service1.pojo.bo.ApplicationBO;
import com.tongzhou.mes.service1.pojo.entity.Application;
import com.tongzhou.mes.service1.vo.app.ApplicationVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 应用对象转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicationConverter {

    Application form2Entity(ApplicationForm appForm);

    Page<ApplicationVO> bo2Vo(Page<ApplicationBO> bo);

}
