package com.alibaba.datax.plugin.writer.elasticsearchwriter;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.common.util.RetryUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

public class ESWriter extends Writer {
    private final static String WRITE_COLUMNS = "write_columns";

    public static class Job extends Writer.Job {
        private static final Logger log = LoggerFactory.getLogger(Job.class);

        private Configuration conf = null;

        @Override
        public void init() {
            this.conf = super.getPluginJobConf();
        }

        @Override
        public void prepare() {
            super.prepare();
        }
        @Override
        public List<Configuration> split(int mandatoryNumber) {
            List<Configuration> configurations = new ArrayList<Configuration>(mandatoryNumber);
            for (int i = 0; i < mandatoryNumber; i++) {
                configurations.add(conf);
            }
            return configurations;
        }

        @Override
        public void post() {
          super.post();
        }

        @Override
        public void destroy() {

        }
    }

    public static class Task extends Writer.Task {

        private static final Logger log = LoggerFactory.getLogger(Job.class);

        private Configuration conf;


        private Configuration writerSliceConfiguration = null;

        private String esClusterName = null;

        private String esClusterIP = null;

        private Integer esClusterPort = null;

        private Boolean esEnableSniff = null;

        private String esIndex = null;

        private String esType = null;

        private String attributeNameString = null;

        private String attributeNameSplit = null;

        private String[] attributeNames = null;

        private String className = null;

        private RestHighLevelClient restHighLevelClient =null;
        /**
         * Gson序列化的时候限制格式，使用GsonBuilder
         */
        private Gson gson = null;
        private JsonParser jsonParser = null;

        private TransportClient client = null;

        private Integer batchSize = null;

        private static final Logger LOG = LoggerFactory.getLogger(Task.class);
        //yyyy-MM-dd'T'HH:mm:ss.SSSZZ
        private DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        private String urlFieldToParseJson = null;
        private String timeField = null;
        @Override
        public void init() {
                this.writerSliceConfiguration = super.getPluginJobConf();
                this.esClusterName = writerSliceConfiguration.getString(Key.esClusterName);
                this.esClusterIP = writerSliceConfiguration.getString(Key.esClusterIP);
                this.esClusterPort = writerSliceConfiguration.getInt(Key.esClusterPort, 9200);
                this.esEnableSniff = writerSliceConfiguration.getBool(Key.esEnableSniff, false);
                this.esIndex = writerSliceConfiguration.getString(Key.esIndex);
                this.esType = writerSliceConfiguration.getString(Key.esType);
                this.attributeNameString = writerSliceConfiguration.getString(Key.attributeNameString);
                this.attributeNameSplit = writerSliceConfiguration.getString(Key.attributeNameSplit, ",");
                attributeNames = attributeNameString.split(attributeNameSplit);
                this.className = writerSliceConfiguration.getString(Key.className);
                this.batchSize = writerSliceConfiguration.getInt(Key.batchSize, 1000);
                this.gson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd HH:mm:ss")
                        .create();
                this.jsonParser = new JsonParser();
                this.urlFieldToParseJson = writerSliceConfiguration.getString(Key.urlFieldToParseJson, "");
                this.timeField = writerSliceConfiguration.getString(Key.timeField, "");
        }

        @Override
        public void prepare() {
            super.prepare();
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(esClusterIP, esClusterPort)));
        }

        @Override
        public void startWrite(RecordReceiver recordReceiver) {
            List<Record> writerBuffer = new ArrayList<Record>(this.batchSize);
            Record record = null;
            while ((record = recordReceiver.getFromReader()) != null) {
                writerBuffer.add(record);
                if (writerBuffer.size() >= this.batchSize) {
                    bulkSaveOrUpdateES(writerBuffer);
                    writerBuffer.clear();
                }
            }
            if (!writerBuffer.isEmpty()) {
                bulkSaveOrUpdateES(writerBuffer);
                writerBuffer.clear();
            }
        }
        private void bulkSaveOrUpdateES(List<Record> writerBuffer) {
            Record record;
            Object object;
            Object value = null;
            Map<String, String> attributeValueMap;
            List<EsEntity> entities = new ArrayList<EsEntity>();
            try {
                for (int w = 0, wlen = writerBuffer.size(); w < wlen; w++) {
                    //此时获取到record为DataX中的数据类型
                    record = writerBuffer.get(w);
                    object = Class.forName(className).newInstance();
                    int fieldNum = record.getColumnNumber();
                    if (null != record && fieldNum > 0) {
                        attributeValueMap = new HashMap<String, String>();
                        for (int i = 0; i < fieldNum; i++) {
                            attributeValueMap.put(attributeNames[i].toLowerCase(), record.getColumn(i).asString());
                        }
                        Class<?> superClass = object.getClass();
                        Field[] fields = superClass.getDeclaredFields();
                        for (Field field : fields) {
                            String fieldNameLowerCase = field.getName().toLowerCase();
                            //如果配置中未填写类的该列字段，跳过
                            if (!attributeValueMap.containsKey(fieldNameLowerCase)) {
                                continue;
                            }
                            String valueString = attributeValueMap.get(fieldNameLowerCase);
                            try {
                                //如果是需要解析Json的字段，解析为Json
                                if (this.urlFieldToParseJson.equals(fieldNameLowerCase)) {
                                    String[] paramTuples = valueString.split("&");
                                    JSONObject jsonObject = new JSONObject();
                                    for (String singleTuple : paramTuples) {
                                        jsonObject.put(singleTuple.split("=", -1)[0], singleTuple.split("=", -1)[1]);
                                    }
                                    value = jsonObject;
                                } else {
                                    value = convertValueByFieldType(field.getType(), valueString);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // if field is private or public
                            if (field.isAccessible()) {
                                field.set(object, value);
                            } else {
                                field.setAccessible(true);
                                field.set(object, value);
                                field.setAccessible(false);
                            }
                        }
                        entities.add((EsEntity) object);
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            bulkSaveOrUpdate(entities, esIndex, esType);
        }

        private void bulkSaveOrUpdate(List<EsEntity> entities, String esIndex, String esType) {
            if (null == entities || entities.isEmpty()) {
                return;
            }
            BulkRequest request = new BulkRequest();
            ObjectMapper mapper = new ObjectMapper();
            for (EsEntity entity : entities) {
                IndexRequest indexRequest = new IndexRequest(esIndex, esType, entity.getId() + "");
                try {
                    indexRequest.source(mapper.writeValueAsString(entity), XContentType.JSON);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                request.add(indexRequest);


            }
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = null;
            try {
                bulkResponse = restHighLevelClient.bulk(request);
            } catch (IOException e) {
                System.out.println("es同步失败!");
            }
            RestStatus restStatus =bulkResponse.status();
            System.out.println(restStatus.getStatus());
            System.out.println(restStatus.name());
            System.out.println("*****");
        }

        private Object convertValueByFieldType(Class<?> type, Object value) throws ParseException {
            Object finalValue = value;
            if (String.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? null : String.valueOf(value);
            } else if (Boolean.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? Boolean.FALSE : Boolean.parseBoolean(String.valueOf(value));
            } else if (Integer.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? 0 : Integer.parseInt(String.valueOf(value));
            } else if (Long.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? 0 : Long.parseLong(String.valueOf(value));
            } else if (Float.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? 0 : Float.parseFloat(String.valueOf(value));
            } else if (Double.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? 0 : Double.parseDouble(String.valueOf(value));
            } else if (Date.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? null : format.parse((String)value);
            } else if (BigDecimal.class.isAssignableFrom(type)) {
                finalValue = (null == value) ? new BigDecimal("0") : new BigDecimal(String.valueOf(value));
            }
            return finalValue;
        }
        private String getDateStr(ESColumn esColumn, Column column) {
            DateTime date = null;
            DateTimeZone dtz = DateTimeZone.getDefault();
            if (esColumn.getTimezone() != null) {
                // 所有时区参考 http://www.joda.org/joda-time/timezones.html
                dtz = DateTimeZone.forID(esColumn.getTimezone());
            }
            if (column.getType() != Column.Type.DATE && esColumn.getFormat() != null) {
                DateTimeFormatter formatter = DateTimeFormat.forPattern(esColumn.getFormat());
                date = formatter.withZone(dtz).parseDateTime(column.asString());
                return date.toString();
            } else if (column.getType() == Column.Type.DATE) {
                date = new DateTime(column.asLong(), dtz);
                return date.toString();
            } else {
                return column.asString();
            }
        }


        @Override
        public void post() {
        }

        @Override
        public void destroy() {
            try {
                restHighLevelClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
