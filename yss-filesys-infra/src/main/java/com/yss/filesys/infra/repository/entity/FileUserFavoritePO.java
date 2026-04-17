package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_user_favorites")
public class FileUserFavoritePO {
    private String userId;
    private String fileId;
    private LocalDateTime createdAt;
}
