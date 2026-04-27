import type { CheckUploadCommand } from "@/api/generated/filesys/schemas";
import { getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi } from "@/api";
import { calculateBlobMD5, calculateFileMD5 } from "@/utils/md5";

const generatedFilesysApi = getRefactoredFreeFsWithASubjectMatchJavaStyleLayeredArchitectureApi();

export interface UploadProgressPayload {
  uploadedBytes: number;
  totalBytes: number;
  uploadedChunks: number;
  totalChunks: number;
}

export interface UploadExecutorCallbacks {
  onProgress?: (taskId: string, payload: UploadProgressPayload) => void;
  onStatus?: (
    taskId: string,
    status: "checking" | "uploading" | "merging" | "completed",
  ) => void;
  onError?: (taskId: string, errorMessage: string) => void;
}

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;
const DEFAULT_CONCURRENCY = 3;
const MAX_RETRY_COUNT = 3;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const isNetworkError = (error: unknown) => {
  if (!(error instanceof Error)) return false;
  const message = error.message.toLowerCase();
  return (
    message.includes("network") ||
    message.includes("timeout") ||
    message.includes("abort") ||
    message.includes("connection") ||
    message.includes("fetch")
  );
};

class UploadExecutor {
  private callbacks: UploadExecutorCallbacks | null = null;
  private concurrency = DEFAULT_CONCURRENCY;

  public setCallbacks(callbacks: UploadExecutorCallbacks) {
    this.callbacks = callbacks;
  }

  public setConcurrency(concurrency: number) {
    this.concurrency = Math.max(1, concurrency);
  }

  public getConcurrency() {
    return this.concurrency;
  }

  public calculateChunkCount(fileSize: number, chunkSize: number) {
    return Math.max(1, Math.ceil(fileSize / chunkSize));
  }

  public async start(
    taskId: string,
    file: File,
    chunkSize = DEFAULT_CHUNK_SIZE,
    concurrency = this.concurrency,
  ): Promise<void> {
    const totalChunks = this.calculateChunkCount(file.size, chunkSize);
    const safeConcurrency = Math.max(1, concurrency);

    this.callbacks?.onStatus?.(taskId, "checking");

    const fileMd5 = await calculateFileMD5(file);
    const checkPayload: CheckUploadCommand = { fileMd5, taskId };
    const checkResp = await generatedFilesysApi.checkUpload(checkPayload);
    const checkData = checkResp.data;

    if (checkData?.instantUpload) {
      this.callbacks?.onProgress?.(taskId, {
        uploadedBytes: file.size,
        totalBytes: file.size,
        uploadedChunks: totalChunks,
        totalChunks,
      });
      this.callbacks?.onStatus?.(taskId, "completed");
      return;
    }

    const uploadedChunksResp = await generatedFilesysApi.getUploadedChunks(taskId);
    const uploadedChunks = new Set((uploadedChunksResp.data ?? []).map((index) => Number(index)));

    this.callbacks?.onStatus?.(taskId, "uploading");
    this.callbacks?.onProgress?.(taskId, {
      uploadedBytes: this.calculateUploadedBytes(file, uploadedChunks, chunkSize),
      totalBytes: file.size,
      uploadedChunks: uploadedChunks.size,
      totalChunks,
    });

    await this.uploadChunks({
      file,
      taskId,
      chunkSize,
      totalChunks,
      uploadedChunks,
      concurrency: safeConcurrency,
    });

    this.callbacks?.onStatus?.(taskId, "merging");
    await generatedFilesysApi.merge({ taskId });
    this.callbacks?.onStatus?.(taskId, "completed");
  }

  private calculateUploadedBytes(
    file: File,
    uploadedChunks: Set<number>,
    chunkSize: number,
  ) {
    let uploadedBytes = 0;
    uploadedChunks.forEach((chunkIndex) => {
      const start = chunkIndex * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      uploadedBytes += Math.max(0, end - start);
    });
    return uploadedBytes;
  }

  private async uploadChunks(params: {
    file: File;
    taskId: string;
    chunkSize: number;
    totalChunks: number;
    uploadedChunks: Set<number>;
    concurrency: number;
  }) {
    const { file, taskId, chunkSize, totalChunks, uploadedChunks, concurrency } = params;
    const chunksToUpload: number[] = [];

    for (let index = 0; index < totalChunks; index += 1) {
      if (!uploadedChunks.has(index)) {
        chunksToUpload.push(index);
      }
    }

    if (!chunksToUpload.length) return;

    let currentIndex = 0;
    const worker = async () => {
      while (currentIndex < chunksToUpload.length) {
        const localIndex = currentIndex;
        currentIndex += 1;
        const chunkIndex = chunksToUpload[localIndex];
        await this.uploadChunkWithRetry({
          file,
          taskId,
          chunkSize,
          chunkIndex,
          totalChunks,
          uploadedChunks,
        });
      }
    };

    const workerCount = Math.min(concurrency, chunksToUpload.length);
    await Promise.all(Array.from({ length: workerCount }, () => worker()));
  }

  private async uploadChunkWithRetry(params: {
    file: File;
    taskId: string;
    chunkSize: number;
    chunkIndex: number;
    totalChunks: number;
    uploadedChunks: Set<number>;
  }) {
    const { file, taskId, chunkSize, chunkIndex, totalChunks, uploadedChunks } = params;
    let retryCount = 0;

    while (retryCount <= MAX_RETRY_COUNT) {
      try {
        const start = chunkIndex * chunkSize;
        const end = Math.min(start + chunkSize, file.size);
        const chunkBlob = file.slice(start, end);
        const chunkMd5 = await calculateBlobMD5(chunkBlob);

        await generatedFilesysApi.uploadChunk(
          { file: chunkBlob },
          { taskId, chunkIndex, chunkMd5 },
        );

        uploadedChunks.add(chunkIndex);
        this.callbacks?.onProgress?.(taskId, {
          uploadedBytes: this.calculateUploadedBytes(file, uploadedChunks, chunkSize),
          totalBytes: file.size,
          uploadedChunks: uploadedChunks.size,
          totalChunks,
        });
        return;
      } catch (error) {
        retryCount += 1;
        if (retryCount > MAX_RETRY_COUNT) {
          const finalError = new Error(
            error instanceof Error ? error.message : `分片 ${chunkIndex} 上传失败`,
          );
          this.callbacks?.onError?.(taskId, finalError.message);
          throw finalError;
        }

        await sleep(isNetworkError(error) ? 800 * retryCount : 600 * retryCount);
      }
    }
  }
}

export const uploadExecutor = new UploadExecutor();
