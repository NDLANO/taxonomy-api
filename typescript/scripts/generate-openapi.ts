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
    exportType: true,
    generatePathParams: true,
    transform(schemaObject, meta) {
      if (
        schemaObject.type === "object" &&
        schemaObject.additionalProperties &&
        schemaObject.properties?.empty
      ) {
        // Records generated from HashMaps have additional boolean property empty
        delete schemaObject.properties;
      }
      return undefined;
    },
  });

  const outputPath = `./taxonomy-api-openapi.ts`;
  const output = astToString(ast);

  console.log(`Outputting to ${outputPath}`);
  fs.writeFileSync(outputPath, output);
}

generate_types(process.argv[2]);
