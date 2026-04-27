import customInstance from "@/api/mutator";
import type {
  DefaultPrimitive,
  MoveToRecycleBinCommand,
  PermanentlyDeleteRecycleCommand,
} from "@/api/generated/filesys/schemas";

export const moveToRecycleFiles = (payload: MoveToRecycleBinCommand) => {
  return customInstance<DefaultPrimitive>({
    url: "/files/recycle",
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    data: payload,
  });
};

export const permanentlyDeleteRecycleFiles = (payload: PermanentlyDeleteRecycleCommand) => {
  return customInstance<DefaultPrimitive>({
    url: "/files/recycle/permanent",
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    data: payload,
  });
};

export const cancelFileSharesByIds = (shareIds: string[]) => {
  return customInstance<DefaultPrimitive>({
    url: "/shares",
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    data: shareIds,
  });
};
