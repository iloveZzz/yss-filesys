import { computed, reactive, type Ref } from "vue";
import type { FormSchemaDetailData, RenderField, StorageConfigData, StorageSchemaNode } from "./types";

interface UseStorageSchemaOptions {
  activeDetail: { detail: FormSchemaDetailData };
  formModel: Ref<StorageConfigData>;
}

export const useStorageSchema = ({
  activeDetail,
  formModel,
}: UseStorageSchemaOptions) => {
  const fieldErrors = reactive<Record<string, string>>({});

  const isPlainObject = (value: unknown): value is Record<string, any> =>
    !!value && typeof value === "object" && !Array.isArray(value);

  const cloneValue = <T,>(value: T): T => {
    if (typeof structuredClone === "function") {
      try {
        return structuredClone(isPlainObject(value) || Array.isArray(value) ? value : value);
      } catch {
        // 回退到 JSON 克隆
      }
    }
    return JSON.parse(JSON.stringify(value ?? null)) as T;
  };

  const mergeDeep = (base: StorageConfigData, patch: StorageConfigData): StorageConfigData => {
    const output = cloneValue(base);
    Object.entries(patch).forEach(([key, value]) => {
      const current = output[key];
      if (isPlainObject(current) && isPlainObject(value)) {
        output[key] = mergeDeep(current, value);
        return;
      }
      output[key] = cloneValue(value);
    });
    return output;
  };

  const normalizeStorageConfigData = (value: StorageConfigData): StorageConfigData => {
    if (!isPlainObject(value)) return value;

    const output = cloneValue(value);
    const layout = isPlainObject(output.layout) ? output.layout : undefined;
    const grid = layout && isPlainObject(layout.grid) ? layout.grid : undefined;

    if (!grid) {
      return output;
    }

    const flattened: StorageConfigData = {
      ...grid,
    };

    Object.entries(output).forEach(([key, item]) => {
      if (key === "layout") return;
      if (key === "grid" && isPlainObject(item)) return;
      flattened[key] = item;
    });

    delete flattened.layout;
    delete flattened.grid;
    return flattened;
  };

  const parseConfigData = (configData?: string) => {
    if (!configData) return {};
    try {
      return normalizeStorageConfigData(JSON.parse(configData) as StorageConfigData);
    } catch {
      return {};
    }
  };

  const activeSchema = computed(() => {
    const detail = activeDetail.detail.formSchema;
    return detail?.schema ?? detail ?? {};
  });

  const detailColumns = computed(() => {
    const detail = activeDetail.detail.formDetailOptions ?? {};
    const rawColumns = Number(detail.maxColumns ?? detail.columns ?? 1);
    return Number.isFinite(rawColumns) && rawColumns > 0 ? rawColumns : 1;
  });

  const prettifyKey = (key: string) =>
    key
      .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
      .replace(/[-_]/g, " ")
      .trim()
      .replace(/^\w/, (match) => match.toUpperCase());

  const normalizeOptions = (node: StorageSchemaNode): Array<{ label: string; value: string }> => {
    const enumValues = Array.isArray(node.enum) ? node.enum : [];
    if (enumValues.length > 0) {
      return enumValues.map((item) => {
        if (isPlainObject(item)) {
          const rawValue = item.value ?? item.label ?? item.name ?? item.title ?? JSON.stringify(item);
          return {
            label: String(item.label ?? item.name ?? item.title ?? rawValue),
            value: String(rawValue),
          };
        }
        return { label: String(item), value: String(item) };
      });
    }

    const componentProps = node["x-component-props"];
    if (isPlainObject(componentProps) && Array.isArray(componentProps.options)) {
      return componentProps.options.map((item) => {
        if (isPlainObject(item)) {
          const rawValue = item.value ?? item.label ?? item.name ?? item.title ?? JSON.stringify(item);
          return {
            label: String(item.label ?? item.name ?? item.title ?? rawValue),
            value: String(rawValue),
          };
        }
        return { label: String(item), value: String(item) };
      });
    }

    return [];
  };

  const isFieldRequired = (node: StorageSchemaNode) =>
    Boolean(
      node.required ||
        (isPlainObject(node["x-decorator-props"]) && Boolean(node["x-decorator-props"].required)) ||
        (isPlainObject(node["x-component-props"]) && Boolean(node["x-component-props"].required)),
    );

  const resolveInputType = (node: StorageSchemaNode): RenderField["inputType"] => {
    const component = String(node["x-component"] ?? "").toLowerCase();
    const type = String(node.type ?? "").toLowerCase();
    const options = normalizeOptions(node);

    if (component.includes("textarea") || component.includes("text-area")) return "textarea";
    if (component.includes("password") || type === "password") return "password";
    if (component.includes("switch") || type === "boolean") return "switch";
    if (component.includes("select") || options.length > 0) return "select";
    if (component.includes("number") || type === "number" || type === "integer") return "number";
    return "input";
  };

  const walkSchema = (node: StorageSchemaNode, path: string, target: RenderField[]) => {
    const properties = node.properties ?? {};
    const component = String(node["x-component"] ?? "");
    const isGroupHeader = component.toLowerCase().includes("groupheader");
    const hasChildren = Object.keys(properties).length > 0;
    const isTransparentWrapper = String(node.type ?? "").toLowerCase() === "void" && !isGroupHeader;

    if (isGroupHeader) {
      target.push({
        kind: "group",
        key: path || `group-${target.length}`,
        title:
          String(node["x-component-props"]?.title ?? node.title ?? node.description ?? "分组") ||
          "分组",
        description: node.description ? String(node.description) : undefined,
      });
      return;
    }

    if (hasChildren) {
      Object.entries(properties).forEach(([key, child]) => {
        const childIsTransparentWrapper =
          String(child.type ?? "").toLowerCase() === "void" &&
          !String(child["x-component"] ?? "").toLowerCase().includes("groupheader");
        const nextPath = childIsTransparentWrapper ? path : path ? `${path}.${key}` : key;
        walkSchema(child, nextPath, target);
      });
      return;
    }

    if (isTransparentWrapper) {
      return;
    }

    const title = String(node.title ?? prettifyKey(path.split(".").at(-1) ?? path));
    const options = normalizeOptions(node);
    const gridSpan = Number(node["x-decorator-props"]?.gridSpan ?? 1);
    target.push({
      kind: "field",
      key: path || `field-${target.length}`,
      path,
      title,
      description: node.description ? String(node.description) : undefined,
      required: isFieldRequired(node),
      placeholder: String(
        node["x-component-props"]?.placeholder ??
          (resolveInputType(node) === "select"
            ? `请选择${title}`
            : resolveInputType(node) === "switch"
              ? ""
              : `请输入${title}`),
      ),
      options,
      inputType: resolveInputType(node),
      gridSpan,
      fieldNode: node,
    });
  };

  const renderFields = computed<RenderField[]>(() => {
    const result: RenderField[] = [];
    walkSchema(activeSchema.value as StorageSchemaNode, "", result);
    return result;
  });

  const clearFieldErrors = () => {
    Object.keys(fieldErrors).forEach((key) => {
      delete fieldErrors[key];
    });
  };

  const isEmptyFieldValue = (value: unknown) =>
    value === undefined || value === null || value === "";

  const getValueByPath = (source: StorageConfigData, path?: string): any => {
    if (!path) return source;
    return path.split(".").reduce<unknown>((acc, segment) => {
      if (!isPlainObject(acc)) return undefined;
      return acc[segment];
    }, source);
  };

  const setValueByPath = (source: StorageConfigData, path: string, value: unknown) => {
    const segments = path.split(".").filter(Boolean);
    if (!segments.length) return source;

    let cursor: StorageConfigData = source;
    segments.forEach((segment, index) => {
      const isLast = index === segments.length - 1;
      if (isLast) {
        cursor[segment] = value;
        return;
      }

      const existing = cursor[segment];
      if (!isPlainObject(existing)) {
        cursor[segment] = {};
      }
      cursor = cursor[segment] as StorageConfigData;
    });

    return source;
  };

  const updateFieldValue = (path: string, value: unknown) => {
    if (!formModel.value) {
      formModel.value = {};
    }
    setValueByPath(formModel.value, path, value);
    if (fieldErrors[path]) {
      delete fieldErrors[path];
    }
  };

  const getNamePath = (path?: string) => {
    if (!path) return [];
    return path.split(".").filter(Boolean);
  };

  const validateDynamicFields = () => {
    clearFieldErrors();

    for (const item of renderFields.value) {
      if (item.kind !== "field" || !item.path || !item.required) continue;

      const value = getValueByPath(formModel.value, item.path);
      if (isEmptyFieldValue(value)) continue;

      fieldErrors[item.path] = `请填写${item.title || "该字段"}`;
    }

    return Object.keys(fieldErrors).length === 0;
  };

  return {
    fieldErrors,
    activeSchema,
    detailColumns,
    renderFields,
    clearFieldErrors,
    isEmptyFieldValue,
    validateDynamicFields,
    getValueByPath,
    updateFieldValue,
    getNamePath,
    parseConfigData,
    normalizeStorageConfigData,
    mergeDeep,
  };
};
