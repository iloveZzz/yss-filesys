<script setup lang="ts">
import { InfoCircleOutlined } from "@ant-design/icons-vue";
import { ref } from "vue";
import ConfigFooter from "./ConfigFooter.vue";
import ConfigHeader from "./ConfigHeader.vue";
import SchemaFields from "./SchemaFields.vue";

type ModalMode = "create" | "edit" | "view";

type StorageConfigData = Record<string, any>;

type PlatformOption = {
  label: string;
  value: string;
  description: string;
};

type StorageSchemaNode = {
  [key: string]: unknown;
};

type RenderField = {
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
};

defineProps<{
  open: boolean;
  mode: ModalMode;
  saveLoading: boolean;
  detailLoading: boolean;
  isCompactMode: boolean;
  schemaVersion: number;
  form: {
    id: string;
    platformIdentifier: string;
    remark: string;
    enabled: number;
  };
  formModel: StorageConfigData;
  platformOptions: PlatformOption[];
  selectedPlatform?: PlatformOption;
  currentStatus: string;
  detailColumns: number;
  renderFields: RenderField[];
  fieldErrors: Record<string, string>;
  getValueByPath: (source: StorageConfigData, path?: string) => unknown;
  updateFieldValue: (path: string, value: unknown) => void;
  getNamePath: (path?: string) => string[];
}>();

const innerFormRef = ref<{ clearValidate?: () => Promise<void> | void } | null>(null);

defineExpose({
  clearValidate: () => innerFormRef.value?.clearValidate?.(),
});

defineEmits<{
  cancel: [];
  save: [];
  updatePlatform: [value: string];
}>();
</script>

<template>
  <a-modal
    :open="open"
    :title="
      mode === 'view'
        ? '查看存储平台配置'
        : form.id
          ? '编辑存储平台配置'
          : '添加存储平台配置'
    "
    :width="880"
    :footer="null"
    centered
    destroy-on-close
    wrap-class-name="storage-modal-wrap"
    class="storage-modal workspace-modal"
    @cancel="$emit('cancel')"
  >
    <div class="storage-modal-body" :class="{ 'is-compact': isCompactMode }">
      <a-form
        ref="innerFormRef"
        :key="`${form.platformIdentifier}-${mode}-${schemaVersion}`"
        :model="formModel"
        layout="vertical"
        size="small"
      >
        <div class="storage-modal-kicker">存储配置</div>
        <ConfigHeader
          :mode="mode"
          :platform-identifier="form.platformIdentifier"
          :platform-options="platformOptions"
          :selected-platform="selectedPlatform"
          :current-status="currentStatus"
          @update-platform="$emit('updatePlatform', $event)"
        />

        <div class="storage-schema-card">
          <SchemaFields
            :loading="detailLoading"
            :mode="mode"
            :detail-columns="detailColumns"
            :render-fields="renderFields"
            :field-errors="fieldErrors"
            :form-model="formModel"
            :get-value-by-path="getValueByPath"
            :update-field-value="updateFieldValue"
            :get-name-path="getNamePath"
          />
        </div>

        <div class="storage-modal-note">
          <InfoCircleOutlined />
          <span>强烈建议添加备注，例如“生产环境”或“测试环境”，便于在切换时快速识别。</span>
        </div>

        <a-alert
          class="platform-warning"
          type="warning"
          show-icon
          :message="`您已配置过 ${selectedPlatform?.label || '该平台'}，强烈建议填写备注以便区分！`"
        />

        <ConfigFooter
          :mode="mode"
          :save-loading="saveLoading"
          @cancel="$emit('cancel')"
          @save="$emit('save')"
        />
      </a-form>
    </div>
  </a-modal>
</template>
