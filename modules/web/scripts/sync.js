import fs from "node:fs";
import { URL } from "node:url";

const LEGADO_ASSETS_WEB_VUE_DIR = new URL(
  "../../../app/src/main/assets/web/vue/",
  import.meta.url,
);
const VUE_DIST_DIR = new URL("../dist/", import.meta.url);

console.log("> delete", LEGADO_ASSETS_WEB_VUE_DIR.pathname);
fs.rmSync(LEGADO_ASSETS_WEB_VUE_DIR, {
  force: true,
  recursive: true,
});

console.log("> mkdir", LEGADO_ASSETS_WEB_VUE_DIR.pathname);
fs.mkdirSync(LEGADO_ASSETS_WEB_VUE_DIR, {
  recursive: true,
});

console.log("> cp dist files");
fs.cpSync(VUE_DIST_DIR, LEGADO_ASSETS_WEB_VUE_DIR, {
  recursive: true,
});

console.log("> cp success");
