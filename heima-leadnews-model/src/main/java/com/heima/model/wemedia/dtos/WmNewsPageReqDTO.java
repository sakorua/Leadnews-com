package com.heima.model.wemedia.dtos;
import com.heima.model.common.dtos.PageRequestDTO;
import lombok.Data;
import java.util.Date;
@Data
public class WmNewsPageReqDTO extends PageRequestDTO {
    //状态
    private Short status;
    //开始时间
    private Date beginPubDate;
    //结束时间
    private Date endPubDate;
    //所属频道ID
    private Integer channelId;
    //关键字
    private String keyword;
}