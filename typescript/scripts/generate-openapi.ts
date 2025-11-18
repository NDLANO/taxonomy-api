/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

import fs from "node:fs";
import openapiTS, { astToString, TransformObject } from "openapi-typescript";
import ts, { TypeNode } from "typescript";

if (process.argv.length !== 2) {
  throw new Error("Invalid use");
}

const BLOB = ts.factory.createTypeReferenceNode(
  ts.factory.createIdentifier("Blob"),
);
const NULL = ts.factory.createLiteralTypeNode(ts.factory.createNull()); // `null`

async function generate_types(appName: string) {
  const jsonFile = `./openapi.json`;
  console.log(`Parsing ${jsonFile} to generate typescript files...`);
  const schema = await fs.promises.readFile(jsonFile, "utf8");
  const schemaContent = JSON.parse(schema);

  const ast = await openapiTS(schemaContent, {
    defaultNonNullable: false,
    exportType: true,
    transform(schemaObject, meta) {
      if (
        schemaObject.type === "object" &&
        schemaObject.additionalProperties &&
        schemaObject.properties?.empty
      ) {
        // Records generated from LanguageFieldDTO have additional boolean property empty
        delete schemaObject.properties;
      }
      return undefined;
    },
  });

  const outputPath = `./taxonomy-api-openapi.ts`;

  const output = astToString(ast);

  console.log(`Outputting to ${outputPath}`);

  fs.writeFileSync(outputPath, output);

  const header = `// This file is generated automatically. Do not edit.
`;
  let newFileContent = `${header}import * as openapi from "./taxonomy-api-openapi";
type schemas = openapi.components["schemas"];
export { openapi };

`;

  const schemas = schemaContent.components.schemas;
  const schemaNames = Object.keys(schemas);
  for (const schemaName of schemaNames) {
    newFileContent += `export type ${schemaName} = schemas["${schemaName}"];\n`;
  }

  const newFilePath = `./taxonomy-api.ts`;

  console.log(`Outputting to ${newFilePath}`);

  fs.writeFileSync(newFilePath, newFileContent);
}

generate_types(process.argv[2]);
