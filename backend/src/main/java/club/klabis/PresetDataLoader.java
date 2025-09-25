package club.klabis;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Can be used to populate data repositories with some example data after application starts. This shall be removed once we move to some persistent storage in later phase of development.
 */
public interface PresetDataLoader {
    public void loadData() throws Exception;

    default public <T> List<T> loadObjectList(Class<T> type, Resource resourceWithData) throws IOException {
        return loadObjectList(type, resourceWithData.getInputStream());
    }

    default public <T> List<T> loadObjectList(Class<T> type, InputStream inputData) throws IOException {
        CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
        CsvMapper mapper = CsvMapper.builder().enable(CsvParser.Feature.EMPTY_STRING_AS_NULL).build();
        MappingIterator<T> readValues = mapper.readerFor(type).with(bootstrapSchema).readValues(inputData);
        return readValues.readAll();
    }


}
