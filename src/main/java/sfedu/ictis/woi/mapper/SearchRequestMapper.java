package sfedu.ictis.woi.mapper;

import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.dto.SearchRequestDTO;

public class SearchRequestMapper {
    public static SearchRequestDTO toDTO(SearchRequest request) {
        if (request == null) {
            return null;
        }

        return new SearchRequestDTO(
                request.getP1(),
                request.getP2(),
                request.getMaxTime()
        );
    }
}