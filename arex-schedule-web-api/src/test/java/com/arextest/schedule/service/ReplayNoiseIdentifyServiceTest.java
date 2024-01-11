package com.arextest.schedule.service;

import com.alibaba.fastjson2.JSON;
import com.arextest.common.utils.CompressionUtils;
import com.arextest.common.utils.SerializationUtils;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import com.arextest.schedule.service.noise.AsyncNoiseCaseAnalysisTaskRunnable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ReplayNoiseIdentifyServiceTest {

  @InjectMocks
  AsyncNoiseCaseAnalysisTaskRunnable runnable;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }
  @Test
  public void testAnalysisNoiseFromCompareResult()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    Method method =
        AsyncNoiseCaseAnalysisTaskRunnable.class.getDeclaredMethod("analysisNoiseFromCompareResult",
            List.class);
    method.setAccessible(true);

    String json1 = "{\n" + "    \"planId\": \"6513d8f98303f22897d46740-noisehandler\",\n"
        + "    \"planItemId\": \"6513d8f98303f22897d46746-noisehandler\",\n"
        + "    \"caseId\": \"6513d8fb8303f22897d4674f\",\n"
        + "    \"operationId\": \"6513d68956b5db64014aaecc\",\n"
        + "    \"serviceName\": \"unknown service key\",\n" + "    \"categoryName\": \"Servlet\",\n"
        + "    \"operationName\": \"/random/randomBaseDataDeep\",\n"
        + "    \"replayId\": \"AREX-192-168-228-0-10994425334-noisehandler\",\n"
        + "    \"recordId\": \"AREX-192-168-228-0-1093157852-noisehandler\",\n"
        + "    \"baseMsg\": \"KLUv/QBYXA8ANiReH2Bp2gGDq8D/PoaUXqcJMSUUBFGE7Ca7WR9pZQzDYAlgAFUATgALhTkynRNLgcIcf3cWCog5EpvxF9kf/zIvwJSCMJZkDn7kdOcAQ4uyzGESTAIVJOZQCgUtZVkQczSbtTojptku+h0CxKKY45N9MU8NRkEIC8QBoRKHojAQBpOUQ0FiFgKZ6tjraN6pbof8dMTctNVzw8fN/Gc+xTsCDfOLMrddOepJNOQwmp6kkp9EMvW/04HBMIfp/wBXw6JhD6DxvJ1EQ3NggBYKZXBYmMMoVke1WuVaqVYGqdenlWqL09OXHnIpU5cfvw+dT/NYvxn7U8FILI7vUpn7794bs/Hu9PbN8GgYbD9e68lbz98hh1PqQds/v6s8Sff7jPZUf3c3uyqj46VpDcMIvztailRy0JQfs6RnG/G8uyP2lLAiQtjWP1KOl3eH3apsqcqJvH2czm29iJq7s3Oi277i6tGzs3vzG2iYXfboUk89Oaf655KkZynyEywgkCIhomAPdCoQH5qKB/fAWHa8Yw4jNud52Ijp1uz20KOKEC/h4oxjgDBqXUQnBDnjjibwZPTrqXzm+XcjSY5jLBCju5diglLchEQp7DL+PhtHmNeVzxyBgQiaIe1aKkeObxbW8GshxsTBG48FlgMBAAA=\",\n"
        + "    \"testMsg\": \"KLUv/QBYNA8ABiReHXBp2iAa9p9hrQoLXFpKgej/w3JTUlKE/3lMVQ0wXwBXAE4AinIcZE6TSAgU5Tg1C4RUDu529+OIrf78hWNIYiSrHPmdv0cLhxKklaMMlAQiUOVgCgQ1JElSOZJ7DxVPnh7FTs8BRIKU41z266caCoIOFogDgiUOBGEgDGUhBwJVEgKPz+/4t4wnMRIL3sT8P82P7XfyETE9jx8xoFHSWds9+7syra1kbqU17BIyKuGhtdaBwShHyf7CWQOngRHA0/wZzctiUIAWClZwWJSjMOWMFxGqdIw1XQTXtDljWutcz1GOpwcPM9cRDzojdy83/rGe5s166M7sjKh+ftczTxvN0SjY0rd2n54tk4vJnPI1e5Qb5cpxn136v+r1t5Iube/n3NU9uSeNgtjVWoiqnI2ylUyiXuOp1WBThd4tHd1ivaSo2G1q91nv++tfVT+bze556Ie45Vv/06yJz4+c+smNxwg0yjnKpgupbmf+DzKtvwZxakYxDyogoCIhokAPdCocJEs5jDPcunJ3d9dHmbA5XF4zGznnpxvLwbhGIjrRT2LhFPu7qXzmqYY6SFEcM9KoR3Zrx5A6rsYmYTSkSlaX4uSjzITQcnnXDcAn0DRpN1A+cnyzsIZfCzEmDuh4LJ4cAQAA\",\n"
        + "    \"logs\": \"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\",\n"
        + "    \"diffResultCode\": 1,\n" + "    \"msgInfo\": {\n" + "        \"msgMiss\": 0\n"
        + "    }\n" + "}";
    ReplayCompareResultCollection dao1 = JSON.parseObject(json1,
        ReplayCompareResultCollection.class);

    String json2 = "{\n" + "    \"planId\": \"6513d8f98303f22897d46740-noisehandler\",\n"
        + "    \"planItemId\": \"6513d8f98303f22897d46746-noisehandler\",\n"
        + "    \"caseId\": \"6513d8fb8303f22897d46750\",\n"
        + "    \"operationId\": \"6513d68956b5db64014aaecc\",\n"
        + "    \"serviceName\": \"unknown service key\",\n" + "    \"categoryName\": \"Servlet\",\n"
        + "    \"operationName\": \"/random/randomBaseDataDeep\",\n"
        + "    \"replayId\": \"AREX-192-168-228-0-10994435136-noisehandler\",\n"
        + "    \"recordId\": \"AREX-192-168-228-0-1093344379-noisehandler\",\n"
        + "    \"baseMsg\": \"KLUv/QBYPA8ABiReHmBp2gGzFfjf80jpdZqQHSFBENAnu8luXKeVMQyDJV4AVgBQAIdf3hlFomBRDrczjARUjkdMRNe33O/0AFMJaiRSObrdzW0D7CSSVY5yiuIUIOVQi+SsJJFA5eD/t963N0vjTWsCRCIpB+Lt9ecbQ0kOAgNxQCjFkSQNhKGk5FCAFIXIxq2Jttp94OudhueNZt/Mi7pmbe92Agtl15EeRO1imnTl5ixEzb4dc0ehlN7TodEoR0m8gFfDomEPoNG8mUTDcmBwGAtlcGCUo7jCY9S2i5iRlYxFCbeHWCHVCt01H/POM9yrvWq8Z5ttaci3ho2GgpHe+L2fbOf71p5oiYrZx0JRnRxJ63yRZZDSY9K3mnXpmNNfpdjWJk9ELaVp6DgVm9W9u90CCyW3k+4imdYNNnKDcFKzqmZQG66VDrZ3+juK2Hfqsyfzp/c37+OyabO1KWzWRb7FvvXUY9O2+OzGN7BQ0JFjyzrxtTm3kNlmNYnMTTKkGCogkCIhokAP5EzIQ4eBeNQQu3gnghTCR2N/5+KHnV/nUXLOYX96LD8gVXf/6AyAYoIgDGk7lySd6shDkTzYJDl1x3asjI1rS0vmmOfko8mIcLP53A8YKNA0aTdYOXJ8M7GGwcZ4TBy88VhgOQEAAA==\",\n"
        + "    \"testMsg\": \"KLUv/QBYJA8AtmNeH2Bp2gETncD/PoaUXo9RE0UlCAL6ZDfZjeu0MoZhsAReAFYATgBtnXgSS4HCHN9noYDMce1nnN+Z/LtsAbYUxFiUOfYd2x0DbIEsSmIOs5gEUpDMsRiJhbIsyByHdrlrz9iHid9MgFgUc6T/4V32MRgFQVggDoiVOBSFgTAYpRwKklkI+Zd+a7mesK55nfbObI9+yLtoeNd5Ag2jnBLq4/VWR235QRbpScnfYeTcdEkdGAxzGGsLeDUsGvYAGs/bSTQ0BwbIQrEMDgtzGARRqlFbIfRpJblQmCOrFZNPrZiokCaSoLXSn+Ouue6ba6q/5jfrPh7o9uIroitidh/nrrUfDZP44l6y1vJ0lCk/hrye1anPe+emV6iL1gp59InwUjOtm/ex2TMNs3gld8gmnOxkJuiYr55DknUYEaU62VyFXiXrPnMl+rl7/HTUX2RmdeY93OQDhYIkePp3e6vvmK6of8mf1wMNk5hvobXz5aeRosMp6Wl2OwEqIMAiIZqwB+T8HpNJFAc2bkGxs3TXMge5AW5ZDj+/fHvGqRw0dz3qyNaxN8P6IT+nkhznLyqTOeyooyLVLR2bNNjjplTJDFNOPjkZ0W82PffDxyF8hsTZtC4TlMg7QA08Iw7oeCy6HAEAAA==\",\n"
        + "    \"logs\": \"KLUv/QBY9TEAlvqcIkCLdDjLemq+ZPYDcYsEvUViogx7W2dmyk0pwTRvGUXRNQG0AJMAggANU4qICEfjUTA5TaAxmGQwQU2SJAw1WVRjMGlSCoWERUUgWsAUeTyOxkNSCgVFxC3qoTXQQylU8zwcDLWoxsAGWhNBapKwKMfECA4mpz1KoYtqEAiDpjkYE2OAJGqyKOnBCBwTgyiFipgklETEJLEkJqhJzXEeTSLySCg8rGGCHgkJJ0o0ycMaEq2JoERzGKhFRQ4E0IgepVDqj4moPUXapBQqExf79RW1qCZ5GEKLMEZNEgLvhxNjTdO5umGYZzRHWyIzfbsMM8NRKXJE+loTFb8VP1QmCOZb8UOlPYwkatrxWL9V+Xf5PlMzff3pfquz6HH2Pds9F9ftfn67uIhJzMGIWJNI0xNFTJNIU4s8zGlA0YN5qLv3nI2p+c38/9f793d6vHoNScxBKSJSiiSpqcmiGsygET3PI4EcEyNoTTSwQOC0giaIvPaTcS8RESKBSLA3f2X6ytRO9s7vfeVD3792RETW3WtHzdNcqb02rxv2zCvTHD1nG1dGc9rSYxxN1JWoC1lXqiqtSivXzfvZ7KqPurjl3t1+ycd7ur44zdvVvTgHiAPEl2LrvTPfp2Oyr7deonfefTNvKoVLWACgJm1eIrbi2ru+CroN843T8FVbtnfzyBbjnjmq6YouRQXxGVFhJV89128y83J2ZX71/uXFXOXF3Wq+vsU1JYHrERd3vaf6yua1ybjZuK4LeT0qv2Lf5rMfo4KIqdkwS5nDtMZwlOHGtG4MYjNzFD3R01tRQcTlZUQ8X/vnS5f/Ut3Vu44Xb7VPb3EBgYSocR6ZERGRghQmmdaxBMV4BlJVexJAoDAKhCCEYRiMYygGgUiIMURIiEApJCREQIyR7B6dZnU0B7qJ1oUf/AMrZalndHYi9i9jpaqaI2PRHGwEp79GNtV3LXkSMNfNKVgRZfn2rKs2fS89ohTEXiCNLgk+wdEg6M8YLO/a6lnZHQ4FeeC2nFrTVTEupihIzf3P6D/X8e/KVSGnAknWxRnhHL6KjqjTt03WLBGiBVKq4xRskAXU5ke5iESEPsT/wjKtVi5YQG++i9ZEjIsadMj3Ue39wu/9qNuI6ixVJ941V28BixucHbYdFxCtQVvpFILuGwMqG0JoF8CW1s9lRR6mDk+AoKbw89k6BgKOnFMITih6AQIS+Q7gebdcIzQ8pmPTYYP+66j//8jc6lkLP0QnVKtfYdO+bGXNKA3lxn6K08G0GEd6xAizLdTzf+uObANyZLAOfBoKgFNagoz0pehdA73vkVMyE13s/IfT7rP9Jmfl4ug79Poy6/lt22vEezU0UKYTBdFr12xmh2ieQoRHyopyWOlfoIzE1GWDdY4k4siSXNEWotFzMFvsVMD0A8ObeUwiX8jc4LiRRINrpd7W4ndy5MjadPzDKzVUQAwns7oi4JNqSczO7hzNy2mI4rykW/hFB4fTlKiaPkI3LawlGzzNq/IDIUXdSThpFE7rh9myj7OS29dgI8db7VQtirqAnxiDEAxtoQtdDm4YPEbT772M4w+Nyf9Gd9F5FCTZdkI21+zlW5bH5OXbq9GWuMIweENC7mU003XFF5es1VEYGJvialOMwsdGr+9UfGVc+KTFVIl8QMRSXTuJcokQQhSu8v6OppJpbaJ+9WVmRhkCPmAIt2ghmG80jYwAoty+aCQcqQnhXPIAfpP6OkjBGmDezUkU60rufnnkIrRMULWUyDzmWIB4byZTjBFcfAViIxEsuKIyxCJr2HJR601PTskQL2/H4iygxVqgI4P3BuSJaOABNphTAFSD6DcMHP3iNH/FSq/dADW6MkiqPz84N+RWWHkv7xkK/7C9Y1zcPVq5vdeiyPgH/z34MPQveACYHgQtlgcdDqs7kEzF8BxQo0kmMHI6vYCwqffyqQBKV36FW6vx18YkPsSa1gD7EO8wEDmgtN3F0rXyDksh85f+SXWYpoDsDeR+sUJt26tkUDcPLgX/U8dup/wvlQ+4WR5w2rg7uGWV8Ry40u0SvM9DhMAaeoBG7T9hPpngKMuLVACE5ETH7yrEZvHVxaYz5iJXP/P4QcySfNeCYPUpolJre+AVugK7XTU=\",\n"
        + "    \"diffResultCode\": 1,\n" + "    \"msgInfo\": {\n" + "        \"msgMiss\": 0\n"
        + "    }\n" + "}";
    ReplayCompareResultCollection dao2 = JSON.parseObject(json2,
        ReplayCompareResultCollection.class);

    List<ReplayCompareResult> collect = Arrays.asList(dao1, dao2).stream()
        .map(ReplayCompareResultConverterTest.INSTANCE::boFromDao).collect(Collectors.toList());
    Object invoke = method.invoke(runnable, collect);
  }

  @Mapper
  public interface ReplayCompareResultConverterTest {

    ReplayCompareResultConverterTest INSTANCE = Mappers.getMapper(
        ReplayCompareResultConverterTest.class);

    @Mappings({@Mapping(target = "baseMsg", qualifiedByName = "decompress"),
        @Mapping(target = "testMsg", qualifiedByName = "decompress"),
        @Mapping(target = "logs", qualifiedByName = "decompressLogs")})
    ReplayCompareResult boFromDao(ReplayCompareResultCollection dao);

    @Named("decompressLogs")
    default List<LogEntity> map(String logs) {
      LogEntity[] logEntities = SerializationUtils.useZstdDeserialize(logs, LogEntity[].class);
      if (logEntities == null) {
        return null;
      }
      return Arrays.asList(logEntities);
    }

    @Named("decompress")
    default String decompress(String compressString) {
      return CompressionUtils.useZstdDecompress(compressString);
    }
  }

}
