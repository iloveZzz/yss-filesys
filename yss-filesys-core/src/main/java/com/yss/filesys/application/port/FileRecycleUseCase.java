package com.yss.filesys.application.port;

import com.yss.filesys.application.command.ClearRecycleCommand;
import com.yss.filesys.application.command.PermanentlyDeleteRecycleCommand;
import com.yss.filesys.application.command.RestoreRecycleCommand;

public interface FileRecycleUseCase {

    void restore(RestoreRecycleCommand command);

    void permanentlyDelete(PermanentlyDeleteRecycleCommand command);

    void clearRecycle(ClearRecycleCommand command);

    int purgeExpiredRecycleItems();
}
