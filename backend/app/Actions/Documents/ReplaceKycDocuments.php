<?php

namespace App\Actions\Documents;

use App\Models\RiderDocument;
use App\Models\StoreDocument;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Http\UploadedFile;

/**
 * The document set of a partner application (store or rider). Photos are encoded before the caller opens its
 * transaction ([encode]); inside it, [discard] drops the previous set and [attach] saves the new one. Old files are
 * deleted only after the commit, new files are removed again if the transaction rolls back.
 */
final class ReplaceKycDocuments
{
    public function __construct(private readonly StoreKycImage $images) {}

    /**
     * @param  array<string, UploadedFile>  $files  keyed by document type
     * @return array<string, string> JPEG bytes keyed by document type
     */
    public function encode(array $files): array
    {
        $encoded = [];
        foreach ($files as $type => $file) {
            $encoded[$type] = $this->images->encode($file, strtolower($type));
        }

        return $encoded;
    }

    /**
     * Drops a resubmitted application's previous documents (rows now, files after the commit). Call it before the
     * application may move to another province: documents are tied to (owner, province).
     *
     * @template TDocument of StoreDocument|RiderDocument
     * @template TOwner of Model
     *
     * @param  HasMany<TDocument, TOwner>  $documents
     */
    public function discard(HasMany $documents): void
    {
        $this->images->deleteAfterCommit(array_values(
            $documents->get()->map(fn (StoreDocument|RiderDocument $document): string => $document->storage_path)->all(),
        ));
        $documents->delete();
    }

    /**
     * @template TDocument of StoreDocument|RiderDocument
     * @template TOwner of Model
     *
     * @param  HasMany<TDocument, TOwner>  $documents
     * @param  array<string, string>  $encoded  from [encode]
     */
    public function attach(HasMany $documents, string $provinceId, string $folder, array $encoded): void
    {
        foreach ($encoded as $type => $jpeg) {
            $documents->create([
                'province_id' => $provinceId,
                'doc_type' => $type,
                'storage_path' => $this->images->put($folder, strtolower($type), $jpeg),
            ]);
        }
    }
}
