<script setup lang="ts">
type StorageConfigData = Record<string, any>;

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
  loading: boolean;
  mode: "create" | "edit" | "view";
  detailColumns: number;
  renderFields: RenderField[];
  fieldErrors: Record<string, string>;
  formModel: StorageConfigData;
  getValueByPath: (source: StorageConfigData, path?: string) => unknown;
  updateFieldValue: (path: string, value: unknown) => void;
  getNamePath: (path?: string) => string[];
}>();
</script>

<template>
  <div class="schema-shell">
    <div v-if="loading" class="schema-loading" :style="{ minHeight: `${Math.max(220, renderFields.length * 44)}px` }">
      <a-skeleton active title :paragraph="{ rows: Math.max(4, Math.min(8, renderFields.length || 4)) }" />
    </div>
    <div
      v-else
      class="schema-grid"
      :style="{ gridTemplateColumns: `repeat(${detailColumns}, minmax(0, 1fr))` }"
    >
      <template v-for="item in renderFields" :key="item.key">
        <div v-if="item.kind === 'group'" class="schema-group">
          <div class="schema-group__title">{{ item.title }}</div>
          <div v-if="item.description" class="schema-group__desc">{{ item.description }}</div>
        </div>

        <a-form-item
          v-else
          :name="getNamePath(item.path)"
          :label="item.title"
          :required="item.required"
          :validate-status="fieldErrors[item.path || ''] ? 'error' : ''"
          :help="fieldErrors[item.path || '']"
          :style="{
            gridColumn: item.gridSpan && item.gridSpan > 1 ? `span ${Math.min(item.gridSpan, detailColumns)}` : 'auto',
          }"
        >
          <a-input
            v-if="item.inputType === 'input'"
            :value="String(getValueByPath(formModel, item.path) ?? '')"
            :placeholder="item.placeholder"
            :disabled="mode === 'view'"
            size="small"
            @update:value="(value) => updateFieldValue(item.path || '', value)"
          />

          <a-input-password
            v-else-if="item.inputType === 'password'"
            :value="String(getValueByPath(formModel, item.path) ?? '')"
            :placeholder="item.placeholder"
            :disabled="mode === 'view'"
            size="small"
            @update:value="(value) => updateFieldValue(item.path || '', value)"
          />

          <a-input-number
            v-else-if="item.inputType === 'number'"
            :value="getValueByPath(formModel, item.path) as any"
            :placeholder="item.placeholder"
            :disabled="mode === 'view'"
            class="w-full"
            size="small"
            @update:value="(value) => updateFieldValue(item.path || '', value as number | null)"
          />

          <a-select
            v-else-if="item.inputType === 'select'"
            :value="getValueByPath(formModel, item.path) as any"
            :placeholder="item.placeholder"
            :options="item.options"
            :disabled="mode === 'view'"
            size="small"
            @update:value="(value) => updateFieldValue(item.path || '', value as string | number | null)"
          />

          <a-switch
            v-else-if="item.inputType === 'switch'"
            :checked="Boolean(getValueByPath(formModel, item.path))"
            :disabled="mode === 'view'"
            checked-children="开启"
            un-checked-children="关闭"
            @update:checked="(value) => updateFieldValue(item.path || '', value)"
          />

          <a-textarea
            v-else-if="item.inputType === 'textarea'"
            :value="String(getValueByPath(formModel, item.path) ?? '')"
            :placeholder="item.placeholder"
            :disabled="mode === 'view'"
            :auto-size="{ minRows: 3, maxRows: 8 }"
            @update:value="(value) => updateFieldValue(item.path || '', value)"
          />
        </a-form-item>
      </template>
    </div>
  </div>
</template>
