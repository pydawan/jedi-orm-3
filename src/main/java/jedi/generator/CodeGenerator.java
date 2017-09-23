package jedi.generator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jedi.db.engine.JediEngine;
import jedi.db.models.ForeignKeyField;
import jedi.db.models.ManyToManyField;
import jedi.db.models.Model;
import jedi.db.models.OneToOneField;
import jedi.db.util.TableUtil;
import jedi.types.JediString;

/**
 * @author thiago
 * @version v1.0.0
 * @since v1.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class CodeGenerator {
   
   public static void generateCode(String path) {
      Class clazz;
      Map<String, String> code;
      for (File file : JediEngine.getModelFiles(path)) {
         clazz = JediEngine.getModel(file);
         List<Field> fields = JediEngine.getFields(clazz);
         for (Field field : fields) {
            if (JediEngine.isOneToOneField(field)) {
               code = CodeGenerator.getOneToOneRelationshipCode(field);
               writeCode(new File(code.get("referencedEntityFile")), code.get("referencedEntityCode"));
            } else if (JediEngine.isForeignKeyField(field)) {
               code = CodeGenerator.getForeignKeyRelationshipCode(field);
               writeCode(new File(code.get("referencedEntityFile")), code.get("referencedEntityCode"));
            } else if (JediEngine.isManyToManyField(field)) {
               code = CodeGenerator.getManytoManyRelationshipCode(field);
               writeCode(new File(code.get("referrerEntityFile")), code.get("referrerEntityCode"));
               writeCode(new File(code.get("referencedEntityFile")), code.get("referencedEntityCode"));
            } else {
            
            }
         }
      }
   }
   
   public static void generateCode() {
      CodeGenerator.generateCode(JediEngine.APP_SRC_DIR);
   }
   
   private static void writeCode(File file, String code) {
      file = file == null || !file.exists() ? null : file;
      code = code == null ? "" : code;
      RandomAccessFile randomAccessFile = null;
      if (file != null && !code.isEmpty()) {
         try {
            randomAccessFile = new RandomAccessFile(file.getAbsolutePath(), "rw");
            boolean generateCode = true;
            randomAccessFile.seek(0);
            StringBuilder lines = new StringBuilder();
            String currentLine = "";
            String firstLine = code;
            if (firstLine.contains("\n    // Generated by Jedi ORM\n")) {
               firstLine = firstLine.replace("\n    // Generated by Jedi ORM\n", "");
            }
            firstLine = firstLine.substring(0, firstLine.indexOf("\n"));
            while ((currentLine = randomAccessFile.readLine()) != null) {
               lines.append(currentLine);
               currentLine = currentLine.replace("^\\s.*$", "");
               if (currentLine.contains(firstLine)) {
                  generateCode = false;
               }
            }
            randomAccessFile.seek(randomAccessFile.length() - 2);
            if (generateCode) {
               randomAccessFile.writeBytes(code);
               randomAccessFile.writeBytes("}\n");
            }
         } catch (IOException e) {
            System.err.println(e);
         } finally {
            if (randomAccessFile != null) {
               try {
                  randomAccessFile.close();
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         }
      }
   }
   
   private static Map<String, String> getOneToOneRelationshipCode(Field field) {
      // Referrer entity (entidade referenciadora).
      // Referenced entity (entidade referenciada).
      Map<String, String> code = new HashMap<>();
      code.put("referrerEntityCode", "");
      code.put("referencedEntityCode", "");
      code.put("referrerEntityFile", "");
      code.put("referencedEntityFile", "");
      if (JediEngine.isOneToOneField(field)) {
         StringBuilder referrerEntityCode = new StringBuilder();
         StringBuilder referencedEntityCode = new StringBuilder();
         Class clazz = field.getType();
         code.put("referrerEntityFile", JediEngine.getModelFile(field.getDeclaringClass()).toString());
         code.put("referencedEntityFile", JediEngine.getModelFile(clazz).toString());
         referrerEntityCode.append("\n\n    // Generated by Jedi ORM\n");
         referrerEntityCode.append(String.format("    public %s get%s() {\n", clazz.getSimpleName(), clazz.getSimpleName()));
         referrerEntityCode.append(
            String.format("        return %s.objects.get(\"%s_id\", this.id);\n", clazz.getSimpleName(), TableUtil.getColumnName(clazz)));
         referrerEntityCode.append("    }\n");
         referencedEntityCode.append("\n    // Generated by Jedi ORM\n");
         referencedEntityCode.append(
            String
               .format("    public %s get%s() {\n", field.getDeclaringClass().getSimpleName(), field.getDeclaringClass().getSimpleName()));
         referencedEntityCode.append(
            String.format(
               "        return %s.objects.get(\"%s_id\", this.id);\n",
               field.getDeclaringClass().getSimpleName(),
               TableUtil.getColumnName(clazz)));
         referencedEntityCode.append("    }\n");
         // Storing the generated code in a map.
         code.put("referrerEntityCode", referrerEntityCode.toString());
         code.put("referencedEntityCode", referencedEntityCode.toString());
      }
      return code;
   }
   
   private static Map<String, String> getForeignKeyRelationshipCode(Field field) {
      Map<String, String> code = new HashMap<>();
      code.put("referrerEntityCode", "");
      code.put("referencedEntityCode", "");
      code.put("referrerEntityFile", "");
      code.put("referencedEntityFile", "");
      if (JediEngine.isForeignKeyField(field)) {
         StringBuilder referrerEntityCode = new StringBuilder();
         StringBuilder referencedEntityCode = new StringBuilder();
         Class clazz = field.getType();
         code.put("referrerEntityFile", JediEngine.getModelFile(field.getDeclaringClass()).toString());
         code.put("referencedEntityFile", JediEngine.getModelFile(clazz).toString());
         referrerEntityCode.append("\n    // Generated by Jedi ORM\n");
         referrerEntityCode.append(String.format("    public %s get%s() {\n", clazz.getSimpleName(), clazz.getSimpleName()));
         referrerEntityCode.append(String.format("        return %s;\n", field.getName()));
         referrerEntityCode.append("    }\n");
         referencedEntityCode.append("\n    // Generated by Jedi ORM\n");
         referencedEntityCode.append(
            String.format(
               "    public QuerySet<%s> get%sSet() {\n",
               field.getDeclaringClass().getSimpleName(),
               field.getDeclaringClass().getSimpleName()));
         referencedEntityCode.append(
            String.format(
               "        return %s.objects.getSet(%s.class, this.id);\n",
               field.getDeclaringClass().getSimpleName(),
               clazz.getSimpleName()));
         referencedEntityCode.append("    }\n");
         code.put("referrerEntityCode", referrerEntityCode.toString());
         code.put("referencedEntityCode", referencedEntityCode.toString());
      }
      return code;
   }
   
   private static Map<String, String> getManytoManyRelationshipCode(Field field) {
      Map<String, String> code = new HashMap<>();
      code.put("referrerEntityCode", "");
      code.put("referencedEntityCode", "");
      code.put("referrerEntityFile", "");
      code.put("referencedEntityFile", "");
      if (JediEngine.isManyToManyField(field)) {
         StringBuilder referrerEntityCode = new StringBuilder();
         StringBuilder referencedEntityCode = new StringBuilder();
         Class c = field.getDeclaringClass();
         Class clazz = null;
         Class superClazz = null;
         ParameterizedType genericType = null;
         ManyToManyField annotation = field.getAnnotation(ManyToManyField.class);
         String through = annotation.through().getSimpleName();
         through = Model.class.getSimpleName().equals(through) ? "" : through;
         String model = annotation.model().getSimpleName();
         model = Model.class.getSimpleName().equals(model) ? "" : model;
         if (model.isEmpty()) {
            if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
               genericType = (ParameterizedType) field.getGenericType();
               superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
               if (superClazz == Model.class) {
                  clazz = (Class) genericType.getActualTypeArguments()[0];
                  model = clazz.getSimpleName();
               }
            }
         }
         code.put("referrerEntityFile", JediEngine.getModelFile(c).toString());
         code.put("referencedEntityFile", JediEngine.getModelFile(clazz).toString());
         String references = annotation.references();
         references = references == null ? "" : references.trim();
         if (references.isEmpty()) {
            if (clazz != null) {
               references = TableUtil.getTableName(clazz);
            } else {
               references = TableUtil.getTableName(model);
            }
         }
         if (!through.isEmpty()) {
            referrerEntityCode.append("\n    // Generated by Jedi ORM\n");
            referrerEntityCode.append(String.format("    public QuerySet<%s> get%s() {\n", model, JediString.capitalize(field.getName())));
            referrerEntityCode.append(String.format("        %s = new QuerySet<%s>();\n", field.getName(), model));
            referrerEntityCode.append(String.format("        %s %s = null;\n", model, model.toLowerCase()));
            referrerEntityCode.append(String.format("        for (%s %s : get%sSet()) {\n", through, through.toLowerCase(), through));
            referrerEntityCode.append(
               String.format(
                  "            %s = %s.objects.<%s>get(\"id\", %s.get%s().getId());\n",
                  model.toLowerCase(),
                  model,
                  model,
                  through.toLowerCase(),
                  model));
            referrerEntityCode.append(String.format("            %s.add(%s);\n", field.getName(), model.toLowerCase()));
            referrerEntityCode.append("        }\n");
            referrerEntityCode.append(String.format("        return %s;\n", field.getName()));
            referrerEntityCode.append("    }\n");
         }
         referencedEntityCode.append("\n    // Generated by Jedi ORM\n");
         referencedEntityCode.append(String.format("    public QuerySet<%s> get%sSet() {\n", c.getSimpleName(), c.getSimpleName()));
         if (through.isEmpty()) {
            referencedEntityCode.append(String.format("        return %s.objects.getSet(%s.class, this.id);\n", c.getSimpleName(), model));
         } else {
            referencedEntityCode.append(
               String.format(
                  "        QuerySet<%s> %sList = new QuerySet<%s>();\n",
                  c.getSimpleName(),
                  c.getSimpleName().toLowerCase(),
                  c.getSimpleName()));
            referencedEntityCode.append(String.format("        %s %s = null;\n", c.getSimpleName(), c.getSimpleName().toLowerCase()));
            referencedEntityCode.append(String.format("        for (%s %s : get%sSet()) {\n", through, through.toLowerCase(), through));
            referencedEntityCode.append(
               String.format(
                  "            %s = %s.objects.<%s>get(\"id\", %s.get%s().getId());\n",
                  c.getSimpleName().toLowerCase(),
                  c.getSimpleName(),
                  c.getSimpleName(),
                  through.toLowerCase(),
                  c.getSimpleName()));
            referencedEntityCode
               .append(String.format("            %s.get%s();\n", c.getSimpleName().toLowerCase(), JediString.capitalize(field.getName())));
            referencedEntityCode
               .append(String.format("            %sList.add(%s);\n", c.getSimpleName().toLowerCase(), c.getSimpleName().toLowerCase()));
            referencedEntityCode.append("        }\n");
            referencedEntityCode.append(String.format("        return %sList;\n", c.getSimpleName().toLowerCase()));
         }
         referencedEntityCode.append("    }\n");
         code.put("referrerEntityCode", referrerEntityCode.toString());
         code.put("referencedEntityCode", referencedEntityCode.toString());
      }
      return code;
   }
   
   public static Map<String, String> getRelationshipCode(Field field) {
      Map<String, String> code = new HashMap<>();
      if (JediEngine.isJediField(field)) {
         if (field.getAnnotation(OneToOneField.class) != null) {
            code = CodeGenerator.getOneToOneRelationshipCode(field);
         } else if (field.getAnnotation(ForeignKeyField.class) != null) {
            code = CodeGenerator.getForeignKeyRelationshipCode(field);
         } else if (field.getAnnotation(ManyToManyField.class) != null) {
            code = CodeGenerator.getManytoManyRelationshipCode(field);
         }
      }
      return code;
   }
   
}
