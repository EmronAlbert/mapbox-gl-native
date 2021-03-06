#pragma once

#include <mbgl/style/source.hpp>

namespace mbgl {

class AnnotationSource : public style::Source {
public:
    AnnotationSource();

    void load(FileSource&) final;

private:
    uint16_t getTileSize() const final { return util::tileSize; }
    Range<uint8_t> getZoomRange() final;

    std::unique_ptr<Tile> createTile(const OverscaledTileID&, const style::UpdateParameters&) final;
};

} // namespace mbgl
