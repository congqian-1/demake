package com.tongzhou.mes.service1.converter;

import com.tongzhou.mes.service1.pojo.dto.PrepackageDataDTO;
import com.tongzhou.mes.service1.pojo.dto.ThirdPartyPrepackageResponseDTO;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ThirdPartyPrepackageMapperTest {
    private final ThirdPartyPrepackageMapper mapper = new ThirdPartyPrepackageMapper();

    @Test
    void emptyBoxCodesAndPackageZeroShouldCombineTwoAndEighteenParts() {
        ThirdPartyPrepackageResponseDTO response = response(segment("", 0, "A", 2), segment(null, 0, "B", 18));
        response.getData().get(0).getPrePackageInfo().get(0).getBoxInfoList().get(0).setBoxType2("0410");
        response.getData().get(0).getPrePackageInfo().get(1).getBoxInfoList().get(0).setBoxType2("0000");
        PrepackageDataDTO.PrePackageInfo result = mapper.toPrepackageData(response, "BATCH", "WORK").getPrePackageInfo();
        assertEquals(1, result.getBoxInfoDetails().size());
        PrepackageDataDTO.BoxInfoDetail box = result.getBoxInfoDetails().get(0);
        assertEquals("UNKNOWN-BATCH-WORK", box.getBoxCode());
        assertEquals(1, box.getPackageInfos().size());
        PrepackageDataDTO.PackageInfo pkg = box.getPackageInfos().get(0);
        assertEquals(0, pkg.getPackageNo());
        assertEquals(20, pkg.getPartCount());
        assertEquals(20, pkg.getPartInfos().size());
        assertEquals("A0", pkg.getPartInfos().get(0).getPartCode());
        assertEquals("B17", pkg.getPartInfos().get(19).getPartCode());
        assertEquals("0410", pkg.getBoxType2());
    }

    @Test
    void repeatedPackageWithinOneBoxInfoListShouldMergeEvenWithMissingCount() {
        ThirdPartyPrepackageResponseDTO.PrePackageInfoItem item = segment("BOX", 3, "A", 1);
        ThirdPartyPrepackageResponseDTO.BoxInfoItem next = segment("BOX", 3, "B", 2).getBoxInfoList().get(0);
        next.setPartCount(null);
        item.setBoxInfoList(Arrays.asList(item.getBoxInfoList().get(0), next));
        PrepackageDataDTO.PackageInfo pkg = mapper.toPrepackageData(response(item), "BATCH", "WORK")
            .getPrePackageInfo().getBoxInfoDetails().get(0).getPackageInfos().get(0);
        assertEquals(3, pkg.getPartCount());
        assertEquals(3, pkg.getPartInfos().size());
    }

    @Test
    void distinctBoxesAndPackageNumbersShouldStaySeparate() {
        PrepackageDataDTO.PrePackageInfo result = mapper.toPrepackageData(response(
            segment("BOX-A", 0, "A", 1), segment("BOX-B", 0, "B", 1), segment("BOX-A", 1, "C", 1)),
            "BATCH", "WORK").getPrePackageInfo();
        assertEquals(2, result.getBoxInfoDetails().size());
        assertEquals(2, result.getBoxInfoDetails().get(0).getPackageInfos().size());
        assertEquals(1, result.getBoxInfoDetails().get(1).getPackageInfos().size());
    }

    @Test
    void duplicatePartCodesShouldNotBeSilentlyDiscarded() {
        PrepackageDataDTO.PackageInfo pkg = mapper.toPrepackageData(response(
            segment("BOX", 0, "SAME", 1), segment("BOX", 0, "SAME", 1)), "BATCH", "WORK")
            .getPrePackageInfo().getBoxInfoDetails().get(0).getPackageInfos().get(0);
        assertEquals(2, pkg.getPartInfos().size()); // 保存层仍能检测并拒绝重复板件。
        assertEquals(pkg.getPartInfos().get(0).getPartCode(), pkg.getPartInfos().get(1).getPartCode());
    }

    private ThirdPartyPrepackageResponseDTO response(ThirdPartyPrepackageResponseDTO.PrePackageInfoItem... segments) {
        ThirdPartyPrepackageResponseDTO response = new ThirdPartyPrepackageResponseDTO();
        response.setCode(0);
        ThirdPartyPrepackageResponseDTO.DataItem item = new ThirdPartyPrepackageResponseDTO.DataItem();
        item.setWorkNum("WORK");
        item.setPrePackageInfo(Arrays.asList(segments));
        response.setData(Arrays.asList(item));
        return response;
    }

    private ThirdPartyPrepackageResponseDTO.PrePackageInfoItem segment(String box, Integer packageNo, String prefix, int count) {
        ThirdPartyPrepackageResponseDTO.PrePackageInfoItem item = new ThirdPartyPrepackageResponseDTO.PrePackageInfoItem();
        item.setBoxCode(box);
        ThirdPartyPrepackageResponseDTO.BoxInfoItem pkg = new ThirdPartyPrepackageResponseDTO.BoxInfoItem();
        pkg.setPackageNo(packageNo);
        pkg.setPartCount(count);
        List<ThirdPartyPrepackageResponseDTO.PartInfoItem> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ThirdPartyPrepackageResponseDTO.PartInfoItem part = new ThirdPartyPrepackageResponseDTO.PartInfoItem();
            part.setPartCode(prefix + i);
            parts.add(part);
        }
        pkg.setPartInfoList(parts);
        item.setBoxInfoList(Arrays.asList(pkg));
        return item;
    }
}
