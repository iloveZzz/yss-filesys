package com.yss.filesys.application.port;

import com.yss.filesys.application.command.FavoriteFilesCommand;

public interface FileFavoriteUseCase {

    void favorite(FavoriteFilesCommand command);

    void unfavorite(FavoriteFilesCommand command);

    long count(String userId);
}
