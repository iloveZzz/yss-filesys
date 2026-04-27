import type { StorageSettingDTO } from "@/api/generated/filesys/schemas";

const normalizeSettings = (items: StorageSettingDTO[]) =>
  items.filter((item) => item.enabled !== 0);

export const extractStorageSettings = (response: unknown): StorageSettingDTO[] => {
  const payload = (response as { data?: unknown } | null | undefined)?.data;
  if (Array.isArray(payload)) {
    return normalizeSettings(payload as StorageSettingDTO[]);
  }

  if (payload && typeof payload === "object") {
    const nestedData = (payload as { data?: unknown; list?: unknown }).data;
    if (Array.isArray(nestedData)) {
      return normalizeSettings(nestedData as StorageSettingDTO[]);
    }
    const list = (payload as { list?: unknown }).list;
    if (Array.isArray(list)) {
      return normalizeSettings(list as StorageSettingDTO[]);
    }
  }

  return [];
};

export const pickDefaultStorageSettingId = (
  settings: StorageSettingDTO[],
): string => {
  const local =
    settings.find((item) => item.enabled !== 0 && item.id === "Local") ||
    settings.find((item) => item.enabled !== 0 && /local/i.test(item.platformIdentifier ?? "")) ||
    settings.find((item) => item.enabled !== 0);
  return local?.id || "";
};
