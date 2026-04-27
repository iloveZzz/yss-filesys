import type {
  StorageFormTemplateDTO,
  StoragePlatformDTO,
  StorageSettingDTO,
} from "@/api/generated/filesys/schemas";

export type StorageConfigData = Record<string, any>;

export type ModalMode = "create" | "edit" | "view";

export type PlatformOption = {
  label: string;
  value: string;
  description: string;
};

export interface StorageSchemaNode {
  type?: string;
  title?: string;
  description?: string;
  required?: boolean;
  enum?: unknown[];
  default?: unknown;
  properties?: Record<string, StorageSchemaNode>;
  items?: StorageSchemaNode;
  "x-component"?: string;
  "x-component-props"?: Record<string, any>;
  "x-decorator"?: string;
  "x-decorator-props"?: Record<string, any>;
  [key: string]: unknown;
}

export interface FormSchemaDetailData {
  formSchema?: StorageSchemaNode;
  formData?: StorageConfigData;
  formDetailOptions?: Record<string, any>;
}

export interface RenderField {
  kind: "group" | "field";
  key: string;
  path?: string;
  title?: string;
  description?: string;
  required?: boolean;
  placeholder?: string;
  options?: Array<{ label: string; value: string }>;
  inputType?: "input" | "password" | "select" | "switch" | "number" | "textarea";
  gridSpan?: number;
  fieldNode?: StorageSchemaNode;
}

export type StorageCardItem = StoragePlatformDTO & {
  config?: StorageConfigData;
  setting?: StorageSettingDTO;
  displayName?: string;
};

export type StorageTemplateOptions = StorageFormTemplateDTO[];
